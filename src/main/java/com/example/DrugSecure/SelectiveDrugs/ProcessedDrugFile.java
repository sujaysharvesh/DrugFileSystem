package com.example.DrugSecure.SelectiveDrugs;

import java.util.Map;

public class ProcessedDrugFile {
    private String filename;
    private String csvContent; // With sensitive terms encrypted
    private String encryptionKey;
    private Map<String, Boolean> drugClassification;

    public ProcessedDrugFile(String originalFilename, String csvContent, String encryptionKey, Map<String, Boolean> classification) {
        this.filename = originalFilename;
        this.csvContent = csvContent;
        this.encryptionKey = encryptionKey;
        this.drugClassification = classification;
    }


    public Map<String, Boolean> getDrugClassification() {
        return drugClassification;
    }

    public void setDrugClassification(Map<String, Boolean> drugClassification) {
        this.drugClassification = drugClassification;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public void setCsvContent(String csvContent) {
        this.csvContent = csvContent;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


}
