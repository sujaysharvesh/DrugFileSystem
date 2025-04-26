package com.example.DrugSecure;



public class EncryptedFile {

    private String originalFilename;
    private byte[] encryptedData;
    private String encryptionKey;

    public EncryptedFile(String originalFilename, byte[] encryptedData, String encryptionKey) {
        this.encryptedData = encryptedData;
        this.encryptionKey = encryptionKey;
        this.originalFilename = originalFilename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
