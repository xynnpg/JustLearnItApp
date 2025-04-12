package com.example.justlearnitappp.drive;

import android.content.Context;
import android.util.Log;

import com.example.justlearnitappp.security.CredentialsManager;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DriveService {
    private static final String TAG = "DriveService";
    private final Drive driveService;
    private final Context context;
    private final CredentialsManager credentialsManager;

    public DriveService(Context context) {
        this.context = context;
        try {
            this.credentialsManager = new CredentialsManager(context);
            this.driveService = initializeDriveService();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing DriveService", e);
            throw new RuntimeException("Failed to initialize DriveService", e);
        }
    }

    private Drive initializeDriveService() {
        try {
            Log.d(TAG, "Initializing Drive service...");
            
            String credentialsJson = credentialsManager.getCredentials();
            if (credentialsJson == null) {
                Log.e(TAG, "No credentials found");
                return null;
            }

            InputStream credentialsStream = new ByteArrayInputStream(credentialsJson.getBytes());
            GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

            Drive service = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("JustLearnIt")
                    .build();
            
            // Test the connection using a specific folder ID
            try {
                service.files().get("1osgmsHhvsUeMMtTwfd_xuD-8jYZjoZlr").execute();
                Log.d(TAG, "Drive service initialized and connection test successful");
            } catch (IOException e) {
                Log.e(TAG, "Drive service connection test failed. Please verify the folder ID and service account permissions.", e);
                return null;
            }
            
            return service;
        } catch (IOException e) {
            Log.e(TAG, "Error initializing Drive service", e);
            return null;
        }
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public void syncFiles(String localPath, String folderId, SyncCallback callback) {
        if (driveService == null) {
            callback.onError("Drive service not initialized");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Starting sync process for folder: " + folderId);
                File localDir = new File(localPath);
                if (!localDir.exists() && !localDir.mkdirs()) {
                    callback.onError("Failed to create local directory: " + localPath);
                    return;
                }

                List<DriveFile> driveFiles = listFilesInFolder(folderId);
                if (driveFiles == null) {
                    callback.onError("Failed to list files in Drive folder");
                    return;
                }

                Log.d(TAG, "Found " + driveFiles.size() + " files in Drive folder");
                for (DriveFile file : driveFiles) {
                    try {
                        File localFile = new File(localDir, file.getName());
                        if (!localFile.exists() || localFile.lastModified() < file.getLastModified()) {
                            downloadFile(file.getId(), localFile.getAbsolutePath());
                            Log.d(TAG, "Downloaded/Updated: " + file.getName());
                        } else {
                            Log.d(TAG, "Skipped (up to date): " + file.getName());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing file: " + file.getName(), e);
                    }
                }

                callback.onSuccess("Sync completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during sync process", e);
                callback.onError("Sync failed: " + e.getMessage());
            }
        }).start();
    }

    private List<DriveFile> listFilesInFolder(String folderId) {
        try {
            Log.d(TAG, "Listing files in folder: " + folderId);
            List<DriveFile> files = new ArrayList<>();
            
            // First verify the folder exists
            try {
                driveService.files().get(folderId).execute();
                Log.d(TAG, "Folder exists: " + folderId);
            } catch (IOException e) {
                Log.e(TAG, "Folder does not exist or access denied: " + folderId, e);
                return null;
            }

            Drive.Files.List request = driveService.files().list()
                    .setQ("'" + folderId + "' in parents and trashed = false")
                    .setFields("files(id, name, modifiedTime, mimeType)");

            FileList result = request.execute();
            if (result.getFiles() == null || result.getFiles().isEmpty()) {
                Log.d(TAG, "No files found in folder: " + folderId);
                return files;
            }

            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                files.add(new DriveFile(
                        file.getId(),
                        file.getName(),
                        file.getModifiedTime().getValue()
                ));
                Log.d(TAG, "Found file: " + file.getName() + 
                          " (ID: " + file.getId() + 
                          ", Type: " + file.getMimeType() + ")");
            }

            return files;
        } catch (IOException e) {
            Log.e(TAG, "Error listing files in folder: " + folderId, e);
            return null;
        }
    }

    private void downloadFile(String fileId, String localPath) throws IOException {
        Log.d(TAG, "Starting download of file: " + fileId + " to: " + localPath);
        
        // Verify file exists and is accessible
        try {
            com.google.api.services.drive.model.File file = driveService.files().get(fileId).execute();
            Log.d(TAG, "File verified: " + file.getName() + " (" + file.getMimeType() + ")");
        } catch (IOException e) {
            Log.e(TAG, "File does not exist or access denied: " + fileId, e);
            throw e;
        }

        File localFile = new File(localPath);
        File parentDir = localFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }
        }

        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            Log.d(TAG, "Successfully downloaded file: " + localPath);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading file: " + fileId, e);
            throw e;
        }
    }

    private static class DriveFile {
        private final String id;
        private final String name;
        private final long lastModified;

        public DriveFile(String id, String name, long lastModified) {
            this.id = id;
            this.name = name;
            this.lastModified = lastModified;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public long getLastModified() { return lastModified; }
    }
}