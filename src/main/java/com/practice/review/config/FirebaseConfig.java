package com.practice.review.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void init() throws IOException {
        log.info("Начинаем инициализацию FirebaseApp. Путь к credentials: {}", firebaseCredentialsPath);
        InputStream serviceAccount;
        if (firebaseCredentialsPath.startsWith("classpath:")) {
            String path = firebaseCredentialsPath.replace("classpath:", "");
            serviceAccount = new ClassPathResource(path).getInputStream();
            log.info("Загружаем сервисный аккаунт из classpath: {}", path);
        } else {
            serviceAccount = new FileInputStream(firebaseCredentialsPath);
            log.info("Загружаем сервисный аккаунт по абсолютному пути: {}", firebaseCredentialsPath);
        }

        String jsonString = readFirebaseCredentialsAsString();
        log.info("Содержимое service-account.json:\n {}", jsonString);


        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId("chokak-rostelekom-practice")
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp инициализирован успешно. Проект: {}", options.getProjectId());
        } else {
            log.info("FirebaseApp уже был инициализирован ранее. Существующие приложения: {}", FirebaseApp.getApps().size());
        }
    }

    public static String readFirebaseCredentialsAsString() {
        try (InputStream is = new ClassPathResource("firebase-service-account.json").getInputStream();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {

            return scanner.useDelimiter("\\A").next(); // Читает всё содержимое
        } catch (Exception e) {
            throw new RuntimeException("Не удалось прочитать firebase-service-account.json как строку", e);
        }
    }
}
