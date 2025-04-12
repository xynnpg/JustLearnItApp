package com.example.justlearnitappp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.justlearnitappp.drive.DriveService;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String LOCAL_FOLDER = "JustLearnIt";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private static final String ROOT_FOLDER_ID = "1osgmsHhvsUeMMtTwfd_xuD-8jYZjoZlr";
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
        setContentView(R.layout.activity_main);
        
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_subjects, R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

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
            // Use custom directory on external storage
            File baseDir = new File(Environment.getExternalStorageDirectory(), LOCAL_FOLDER);
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

            // Set up credentials first
            CredentialsSetup.setupCredentials(this);

            // Initialize Drive service on background thread
            executor.execute(() -> {
                try {
                    driveService = new DriveService(MainActivity.this);
                    if (driveService != null) {
                        Log.d(TAG, "DriveService initialized successfully");
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "Drive service initialized", Toast.LENGTH_SHORT).show();
                            syncAllFolders();
                        });
                    } else {
                        Log.e(TAG, "Failed to initialize DriveService");
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "Failed to initialize Drive service", Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing Drive service: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        Toast.makeText(MainActivity.this, "Error initializing Drive service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void syncAllFolders() {
        if (driveService == null) {
            Log.e(TAG, "DriveService is null, cannot sync folders");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting folder sync...");
                
                // Sync images
                if (imagesDir != null) {
                    driveService.syncFiles(IMAGES_FOLDER_ID, imagesDir.getAbsolutePath(), new DriveService.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Images sync completed: " + message);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Images sync failed: " + error);
                        }
                    });
                }

                // Sync lessons
                if (lessonsDir != null) {
                    driveService.syncFiles(LESSONS_FOLDER_ID, lessonsDir.getAbsolutePath(), new DriveService.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Lessons sync completed: " + message);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Lessons sync failed: " + error);
                        }
                    });
                }

                // Sync tests
                if (testsDir != null) {
                    driveService.syncFiles(TESTS_FOLDER_ID, testsDir.getAbsolutePath(), new DriveService.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Tests sync completed: " + message);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Tests sync failed: " + error);
                        }
                    });
                }

                // Sync videos
                if (videosDir != null) {
                    driveService.syncFiles(VIDEOS_FOLDER_ID, videosDir.getAbsolutePath(), new DriveService.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Videos sync completed: " + message);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Videos sync failed: " + error);
                        }
                    });
                }

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "All folders synced successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error syncing folders: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Error syncing folders: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}