package com.rsvpplaner.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectStorage {

    @Value("${minio.host}")
    private String minioHost;

    @Value("${minio.username}")
    private String minioUsername;

    @Value("${minio.password}")
    private String minioPassword;

    @Value("${minio.bucket.eventimages}")
    private String minioBucket;

    @Bean
    public MinioClient minioClient() {
        var client = MinioClient.builder()
                .endpoint(minioHost)
                .credentials(minioUsername, minioPassword)
                .build();

        try {
            var exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(minioBucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return client;
    }
}
