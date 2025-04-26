package com.example.DrugSecure;

import com.example.DrugSecure.SelectiveDrugs.ProcessedDrugFile;
import com.example.DrugSecure.SelectiveDrugs.SelectiveDrugEncryptionService;
import com.example.DrugSecure.userAuth.User.CustomUser.CustomUserDetails;
import com.example.DrugSecure.userAuth.User.CustomUser.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/secure-files")
public class SecureFileController {

    @Autowired
    private FileEncryptionService fileEncryptionService;

    private final S3StorageService s3StorageService;
    private final FileLocalStoreService fileLocalStoreService;

    private final SelectiveDrugEncryptionService selectiveDrugEncryptionService;

    public SecureFileController(S3StorageService s3StorageService,
                                SelectiveDrugEncryptionService selectiveDrugEncryptionService,
                                FileLocalStoreService fileLocalStoreService) {
        this.s3StorageService = s3StorageService;
        this.selectiveDrugEncryptionService = selectiveDrugEncryptionService;
        this.fileLocalStoreService = fileLocalStoreService;
    }

    @GetMapping("/public-files")
    public ResponseEntity<List<DrugFile>> getAllfiles() {
        try {
            List<DrugFile> files = fileLocalStoreService.getAllFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("title") String title,
                                                          @RequestParam("description") String description) {
        try {
            // Process and encrypt file
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            ProcessedDrugFile encryptedFile = selectiveDrugEncryptionService.processCsv(file);

            // Store in S3
            String s3Key = "encrypted-files/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            s3StorageService.uploadEncryptedFile(s3Key, encryptedFile.getCsvContent());
            Long fileSize = file.getSize();
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("s3Key", s3Key);
            response.put("encryptionKey", encryptedFile.getEncryptionKey());
            response.put("filename", file.getOriginalFilename());
            fileLocalStoreService.storeFileMetadata(s3Key,
                    customUserDetails.getId(),
                    encryptedFile.getFilename(),
                    encryptedFile.getEncryptionKey(),
                    title,
                    description,
                    file.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<String> downloadFile(
            @RequestParam("fileId") UUID fileId,
            @RequestParam(required = false) String decryptionKey,
            HttpServletResponse response) {

        try {
            String s3Key = fileLocalStoreService.getS3KeyByFileId(fileId);
            String encryptedContent = s3StorageService.downloadEncryptedFile(s3Key);

            if (decryptionKey == null || decryptionKey.isEmpty()) {
                // Return encrypted file if no key provided
                String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + filename + ".enc\"");
                return ResponseEntity.ok(encryptedContent);
            } else {
                // Decrypt and return original file
                String decryptedContent = selectiveDrugEncryptionService.decryptCsvContent(
                        encryptedContent, decryptionKey);

                String originalFilename = s3Key.substring(s3Key.lastIndexOf("_") + 1);
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + originalFilename + "\"");

                // Set proper content type based on file extension
                String contentType = Files.probeContentType(Paths.get(originalFilename));
                if (contentType != null) {
                    response.setContentType(contentType);
                }

                return ResponseEntity.ok(decryptedContent);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/")
    public ResponseEntity<?> deleteFile(@PathParam("fileId") UUID fileId) {
        try {
            String s3Key = fileLocalStoreService.getS3KeyByFileId(fileId);
            s3StorageService.deleteFile(s3Key);
            if(!fileLocalStoreService.deleteFileMetadata(fileId)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "File metadata deletion failed"));
            }
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File deletion failed: " + e.getMessage()));
        }
    }

    @GetMapping("/getEncryptionKey")
    public ResponseEntity<String> getEncryptionKey(@RequestParam("fileId") UUID fileId,
                                                   @RequestParam("password") String password) {
        try {
            String key = fileLocalStoreService.getEncryptionKey(fileId, password);
            return ResponseEntity.ok().body(key);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving encryption key: " + e.getMessage());
        }
    }
}