package com.example.DrugSecure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;


@Service
public class FileEncryptionService {

    @Autowired
    private DrugClassificationService classificationService;


    public EncryptedFile processFile(MultipartFile file) throws IOException {
        // Generate encryption key
        String encryptionKey = generateEncryptionKey();

        // Encrypt entire file
        byte[] encryptedBytes = encryptFile(file.getBytes(), encryptionKey);

        return new EncryptedFile(
                file.getOriginalFilename(),
                encryptedBytes,
                encryptionKey
        );
    }

    private byte[] encryptFile(byte[] fileBytes, String encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);  // FIX
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] iv = cipher.getIV();
            byte[] encryptedBytes = cipher.doFinal(fileBytes);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(iv);
            outputStream.write(encryptedBytes);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("File encryption failed", e);
        }
    }


    public byte[] decryptFile(byte[] encryptedFile, String encryptionKey) {
        try {
            // Split IV and encrypted data
            ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedFile);
            byte[] iv = new byte[12]; // GCM IV length
            inputStream.read(iv);
            byte[] encryptedData = inputStream.readAllBytes();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("File decryption failed", e);
        }
    }


    private String generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // Use 256-bit AES
            return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }
}
