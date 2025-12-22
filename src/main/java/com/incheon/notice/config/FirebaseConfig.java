package com.incheon.notice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Admin SDK 설정
 * Firebase Authentication 및 FCM 사용을 위한 초기화
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.credentials-path:./firebase-credentials.json}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully with config: {}", firebaseConfigPath);
                return firebaseApp;
            } catch (Exception e) {
                log.error("Failed to initialize Firebase Admin SDK: {}. " +
                        "Please ensure firebase-credentials.json exists at: {}",
                        e.getMessage(), firebaseConfigPath);
                throw e;
            }
        } else {
            log.info("Firebase Admin SDK already initialized");
            return FirebaseApp.getInstance();
        }
    }
}
