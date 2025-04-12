package com.example.justlearnitappp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.justlearnitappp.drive.DriveService;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String LOCAL_FOLDER = "JustLearnIt";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private static final String IMAGES_FOLDER_ID = "1osgmsHhvsUeMMtTwfd_xuD-8jYZjoZlr";
    private static final String LESSONS_FOLDER_ID = "1m4p2px_x-m7wEkg_G0OFk6Ehg4ViVuT5";
    private static final String TESTS_FOLDER_ID = "1U1-ain7IDqoLSQdP-HYwrsmysBH6lULQ";
    private static final String VIDEOS_FOLDER_ID = "1IpL_H3yPz78ENNHX24zRF-JvalXdM6Pu";

    private File imagesDir;
    private File lessonsDir;
    private File testsDir;
    private File videosDir;
    private DriveService driveService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (checkAndRequestPermissions()) {
            initializeApp();
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initializeApp();
            } else {
                Toast.makeText(this, "Required permissions denied. App will exit.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeApp() {
        try {
            Log.d(TAG, "Initializing app...");
            File baseDir = new File(getFilesDir(), LOCAL_FOLDER);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new RuntimeException("Failed to create base directory");
            }

            imagesDir = new File(baseDir, "images");
            lessonsDir = new File(baseDir, "lessons");
            testsDir = new File(baseDir, "tests");
            videosDir = new File(baseDir, "videos");

            File[] dirs = {imagesDir, lessonsDir, testsDir, videosDir};
            for (File dir : dirs) {
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + dir.getName());
                }
            }

            Log.d(TAG, "Base directory: " + baseDir.getAbsolutePath());
            Log.d(TAG, "Images directory: " + imagesDir.getAbsolutePath());
            Log.d(TAG, "Lessons directory: " + lessonsDir.getAbsolutePath());
            Log.d(TAG, "Tests directory: " + testsDir.getAbsolutePath());
            Log.d(TAG, "Videos directory: " + videosDir.getAbsolutePath());

            // Initialize Drive service on background thread
            executor.execute(() -> {
                try {
                    driveService = new DriveService(LoginActivity.this);
                    if (driveService != null) {
                        Log.d(TAG, "DriveService initialized successfully");
                        mainHandler.post(() -> {
                            Toast.makeText(LoginActivity.this, "Drive service initialized", Toast.LENGTH_SHORT).show();
                            syncAllFolders();
                        });
                    } else {
                        Log.e(TAG, "Failed to initialize DriveService");
                        mainHandler.post(() -> {
                            Toast.makeText(LoginActivity.this, "Failed to initialize Drive service", Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing Drive service: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        Toast.makeText(LoginActivity.this, "Error initializing Drive service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void syncAllFolders() {
        Log.d(TAG, "Starting sync of all folders...");
        String[][] folderPairs = {
                {imagesDir.getAbsolutePath(), IMAGES_FOLDER_ID, "Images"},
                {lessonsDir.getAbsolutePath(), LESSONS_FOLDER_ID, "Lessons"},
                {testsDir.getAbsolutePath(), TESTS_FOLDER_ID, "Tests"},
                {videosDir.getAbsolutePath(), VIDEOS_FOLDER_ID, "Videos"}
        };

        for (String[] pair : folderPairs) {
            final String folderName = pair[2];
            Log.d(TAG, "Syncing folder: " + folderName);
            driveService.syncFiles(pair[0], pair[1], new DriveService.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, folderName + " sync successful: " + message);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, folderName + " sync successful", Toast.LENGTH_SHORT).show();
                        // After successful sync, start MainActivity
                        startMainActivity();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, folderName + " sync failed: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, folderName + " sync failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}