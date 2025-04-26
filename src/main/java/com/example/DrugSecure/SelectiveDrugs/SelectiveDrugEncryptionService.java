package com.example.DrugSecure.SelectiveDrugs;

import com.example.DrugSecure.CryptoUtil;
import com.example.DrugSecure.DrugClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.KeyGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SelectiveDrugEncryptionService {

    @Autowired
    private DrugClassificationService classificationService;

    @Autowired
    private CryptoUtil cryptoUtil;

    private static final String ENCRYPTION_PREFIX = "ENC_ROW[";
    private static final String ENCRYPTION_SUFFIX = "]";

    public ProcessedDrugFile processCsv(MultipartFile file) throws IOException {
        List<String[]> rows = cryptoUtil.readCsv(file.getInputStream());
        Map<String, Boolean> classification = new HashMap<>();
        List<String[]> processedRows = new ArrayList<>();

        String encryptionKey = generateEncryptionKey();

        // Header row stays unencrypted
        if (!rows.isEmpty()) {
            processedRows.add(rows.get(0));
        }

        // Process each data row
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            boolean isSensitive = containsSensitiveDrug(row, classification);

            if (isSensitive) {
                processedRows.add(encryptRow(row, encryptionKey));
            } else {
                processedRows.add(row);
            }
        }

        return new ProcessedDrugFile(
                file.getOriginalFilename(),
                cryptoUtil.convertToCsvString(processedRows),
                encryptionKey,
                classification
        );
    }

    private boolean containsSensitiveDrug(String[] row, Map<String, Boolean> classification) {
        for (String cell : row) {
            if (cell == null || cell.trim().isEmpty()) continue;

            Map<String, Boolean> result = classificationService.classifyDrugComponents(cell);

            for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                classification.putIfAbsent(entry.getKey(), entry.getValue());
                if (entry.getValue()) return true; // If any component is sensitive
            }
        }
        return false;
    }

    private String[] encryptRow(String[] row, String encryptionKey) {
        String[] encryptedRow = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            encryptedRow[i] = row[i] == null ? null :
                    ENCRYPTION_PREFIX + cryptoUtil.encrypt(row[i], encryptionKey) + ENCRYPTION_SUFFIX;
        }
        return encryptedRow;
    }

    public String decryptCsvContent(String csvContent, String encryptionKey) throws IOException {
        List<String[]> rows = cryptoUtil.readCsv(new ByteArrayInputStream(csvContent.getBytes()));
        List<String[]> decryptedRows = new ArrayList<>();

        if (!rows.isEmpty()) {
            decryptedRows.add(rows.get(0)); // Header
        }

        for (int i = 1; i < rows.size(); i++) {
            decryptedRows.add(decryptRow(rows.get(i), encryptionKey));
        }

        return cryptoUtil.convertToCsvString(decryptedRows);
    }
    private String[] decryptRow(String[] row, String encryptionKey) {
        String[] decryptedRow = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            if (row[i] != null && isEncrypted(row[i])) {
                try {
                    String encryptedContent = row[i].substring(
                            ENCRYPTION_PREFIX.length(),
                            row[i].length() - ENCRYPTION_SUFFIX.length()
                    );
                    decryptedRow[i] = cryptoUtil.decrypt(encryptedContent, encryptionKey);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid encryption key or corrupted data.");
                }
            } else {
                decryptedRow[i] = row[i];
            }
        }
        return decryptedRow;
    }

    private boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTION_PREFIX) && value.endsWith(ENCRYPTION_SUFFIX);
    }

    private String generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }
}
