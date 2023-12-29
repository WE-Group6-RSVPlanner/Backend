package com.rsvpplaner.config;

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

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioHost)
                .credentials(minioUsername, minioPassword)
                .build();
    }
}
