package com.example.DrugSecure;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;


@Service
public class CryptoUtil {
    // Replace with your actual key
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes (96 bits for GCM)

    public String encrypt(String plaintext, String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] encrypted = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, encrypted, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText, String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);

            // Separate IV and ciphertext
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[encrypted.length - IV_LENGTH];
            System.arraycopy(encrypted, IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            // Decrypt
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // CSV helper methods
    public List<String[]> readCsv(InputStream inputStream) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            return reader.readAll();
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public String convertToCsvString(List<String[]> rows) throws IOException {
        StringWriter writer = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeAll(rows);
        }
        return writer.toString();
    }
}

