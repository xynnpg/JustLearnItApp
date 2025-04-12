package com.example.justlearnitappp.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CredentialsManager {
    private static final String TAG = "CredentialsManager";
    private static final String KEYSTORE_ALIAS = "JustLearnItCredentials";
    private static final String CREDENTIALS_FILE = "encrypted_credentials.dat";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final Context context;
    private final KeyStore keyStore;

    public CredentialsManager(Context context) throws Exception {
        this.context = context;
        this.keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        this.keyStore.load(null);
        initializeKey();
    }

    private void initializeKey() throws Exception {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();

            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }
    }

    public void saveCredentials(String credentials) throws Exception {
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(credentials.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv);
        outputStream.write(encrypted);

        // Save to file
        File file = new File(context.getFilesDir(), CREDENTIALS_FILE);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(outputStream.toByteArray());
        }
    }

    public String getCredentials() throws Exception {
        File file = new File(context.getFilesDir(), CREDENTIALS_FILE);
        if (!file.exists()) {
            return null;
        }

        // Read encrypted data
        byte[] encryptedData;
        try (FileInputStream fis = new FileInputStream(file)) {
            encryptedData = new byte[(int) file.length()];
            fis.read(encryptedData);
        }

        // Split IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        // Decrypt
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public boolean hasCredentials() {
        File file = new File(context.getFilesDir(), CREDENTIALS_FILE);
        return file.exists();
    }

    public void deleteCredentials() {
        File file = new File(context.getFilesDir(), CREDENTIALS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
} 