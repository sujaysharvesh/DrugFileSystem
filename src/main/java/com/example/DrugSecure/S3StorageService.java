package com.example.DrugSecure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


@Service
public class S3StorageService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;

    public S3StorageService(@Value("${aws.access-key}") String accessKey,
                            @Value("${aws.secret-key}") String secretKey,
                            @Value("${aws.region}") String region) {

        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of(region))
                .build();
    }

    public String uploadEncryptedFile(String key, String encryptedContent) {
        try {
            System.out.println("Uploading file to S3...");
            System.out.println("Bucket: " + bucketName);
            System.out.println("Key: " + key);
            System.out.println("Encrypted size: ");

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(encryptedContent));

            System.out.println("Upload successful.");
            return key;
        } catch (Exception e) {
            e.printStackTrace(); // Show root cause
            throw new RuntimeException("Failed to upload encrypted file to S3", e);
        }
    }


    public String downloadEncryptedFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes =
                    s3Client.getObjectAsBytes(getObjectRequest);

            return objectBytes.asUtf8String();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download encrypted file from S3", e);
        }
    }

    public void deleteFile(String key) {
        try {
            System.out.println("Attempting to delete key: " + key + " from bucket: " + bucketName);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception s3e) {
            System.err.println("S3 Error Code: " + s3e.awsErrorDetails().errorCode());
            System.err.println("S3 Error Message: " + s3e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete file from S3", s3e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("General failure while deleting file from S3", e);
        }
    }

}
