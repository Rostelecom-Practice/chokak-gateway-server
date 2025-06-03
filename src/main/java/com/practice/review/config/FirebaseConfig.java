package com.practice.review.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.practice.review.security.FirebaseAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.credentials.path}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void init() throws IOException {
        InputStream serviceAccount;
        if (firebaseCredentialsPath.startsWith("classpath:")) {
            String path = firebaseCredentialsPath.replace("classpath:", "");
            serviceAccount = new ClassPathResource(path).getInputStream();
        } else {
            serviceAccount = new FileInputStream(firebaseCredentialsPath);
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
