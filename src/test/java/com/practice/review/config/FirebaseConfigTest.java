package com.practice.review.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseConfigTest {

    @Test
    void testReadFirebaseCredentialsAsString_ReturnsContent() {
        String json = FirebaseConfig.readFirebaseCredentialsAsString();
        assertNotNull(json, "Метод не должен вернуть null");
        String trimmed = json.trim();
        assertTrue(
                trimmed.startsWith("{") && trimmed.endsWith("}"),
                "Ожидается, что считанный файл — корректный JSON"
        );
    }

    @Test
    void testInit_WithClasspathPrefix_AppNotInitialized_YieldsInitialize() throws Exception {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsPath", "classpath:firebase-service-account.json");

        try (
                var firebaseAppMock = mockStatic(FirebaseApp.class);
                var googleCredsMock = mockStatic(GoogleCredentials.class)
        ) {
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            googleCredsMock
                    .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(mock(GoogleCredentials.class));

            config.init();

            googleCredsMock.verify(() -> GoogleCredentials.fromStream(any(InputStream.class)), times(1));
            firebaseAppMock.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
        }
    }

    @Test
    @DisplayName("init() не должен вызывать initializeApp(), если FirebaseApp.getApps() возвращает непустой список")
    void testInit_WhenAlreadyInitialized_DoesNotCallInitialize() throws Exception {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsPath", "classpath:firebase-service-account.json");

        try (
                var firebaseAppMock = mockStatic(FirebaseApp.class);
                var googleCredsMock = mockStatic(GoogleCredentials.class)
        ) {
            FirebaseApp fakeApp = mock(FirebaseApp.class);
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(fakeApp));

            googleCredsMock
                    .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(mock(GoogleCredentials.class));

            config.init();

            firebaseAppMock.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), never());
            googleCredsMock.verify(() -> GoogleCredentials.fromStream(any(InputStream.class)), times(1));
        }
    }

}
