package com.example.DrugSecure;

public class FileUploadResponse {
    private String fileId;
    private String encryptionKey;
    private String originalFilename;

    public FileUploadResponse(String encryptionKey, String fileId, String originalFilename) {
        this.encryptionKey = encryptionKey;
        this.fileId = fileId;
        this.originalFilename = originalFilename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    // constructor, getters
}