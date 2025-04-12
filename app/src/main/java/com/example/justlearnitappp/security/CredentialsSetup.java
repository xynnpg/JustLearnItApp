package com.example.justlearnitappp.security;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CredentialsSetup {
    private static final String TAG = "CredentialsSetup";
    private static final String CREDENTIALS_FILE_NAME = "drive_credentials.json";

    public static void setupCredentials(Context context) {
        try {
            CredentialsManager credentialsManager = new CredentialsManager(context);
            
            // Check if credentials are already set up
            if (credentialsManager.hasCredentials()) {
                Log.d(TAG, "Credentials already set up");
                return;
            }

            // Read credentials from assets
            String credentialsJson = readCredentialsFromAssets(context);
            if (credentialsJson == null) {
                Log.e(TAG, "Failed to read credentials from assets");
                return;
            }

            // Save encrypted credentials
            credentialsManager.saveCredentials(credentialsJson);
            Log.d(TAG, "Credentials set up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up credentials", e);
        }
    }

    private static String readCredentialsFromAssets(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("credentials/" + CREDENTIALS_FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            
            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading credentials file", e);
            return null;
        }
    }
} 