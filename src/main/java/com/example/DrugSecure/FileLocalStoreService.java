package com.example.DrugSecure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional("drugTransactionManager")
public class FileLocalStoreService {


    @Value("${jwt.secret}")
    private String saltKey;

    @Autowired
    private FileRepository fileRepo;


    private final CryptoUtil cryptoUtil;

    public FileLocalStoreService(CryptoUtil cryptoUtil) {
        this.cryptoUtil = cryptoUtil;
    }

    public List<DrugFile> getAllFiles() {
        return fileRepo.findAll(Sort.by("uploadTime").descending());
    }

    public List<DrugFile> getUserFiles(UUID userId) {
        return fileRepo.findFilesByUserId(userId);
    }

    public String getS3KeyByFileId(UUID fileId) {
        Optional<DrugFile> file = fileRepo.findById(fileId);
        if (file.isPresent()) {
            return file.get().getS3Key();
        } else {
            throw new RuntimeException("File not found");
        }
    }

    public void storeFileMetadata(String s3Key,
                                  UUID userId,
                                  String filename,
                                  String encryptionKey,
                                  String title,
                                  String description,
                                  Long fileSize) {
        DrugFile file = new DrugFile();
        file.setS3Key(s3Key);
        file.setUserId(userId);
        file.setFilename(filename);
        file.setTitle(title);
        file.setDescription(description);
        file.setEncryptionKey(cryptoUtil.encrypt(encryptionKey, saltKey));
        file.setFileSize(fileSize);// Encrypt before storage
        file.setUploadTime(LocalDateTime.now());
        fileRepo.save(file);
    }

    public String getEncryptionKey(UUID fileId, String encryptionKey) {
        DrugFile file = fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File with ID " + fileId + " not found"));

        // Assuming `encryptionKey` is the password provided by the user.
        try {
            return String.valueOf(cryptoUtil.decrypt(file.getEncryptionKey(), encryptionKey)); // Decrypt on retrieval
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt encryption key. Possibly incorrect password.");
        }
    }


    public DrugFile getFileMetadata(UUID fileId) {
        return fileRepo.findById(fileId).orElseThrow();
    }

    public Boolean deleteFileMetadata(UUID fileId) {
        try{
            DrugFile file = fileRepo.findById(fileId).orElseThrow();
            fileRepo.delete(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
