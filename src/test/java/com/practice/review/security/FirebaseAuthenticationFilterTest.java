package com.practice.review.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FirebaseAuthenticationFilterTest {

    private final FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter();

    @Test
    void testNoAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        WebFilterChain chain = e -> {
            capturedExchange.set(e);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange ePassed = capturedExchange.get();
        assertNotNull(ePassed, "filter должен вызвать chain.filter(...)");
        assertFalse(
                ePassed.getRequest().getHeaders().containsKey("X-User-Uid"),
                "При отсутствии Authorization не должно добавляться X-User-Uid"
        );
    }

    @Test
    void testInvalidAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("Authorization", "Basic 12345")
                .header("X-User-Uid", "oldValue")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        WebFilterChain chain = e -> {
            capturedExchange.set(e);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange ePassed = capturedExchange.get();
        assertNotNull(ePassed, "chain.filter должен быть вызван");
        assertFalse(
                ePassed.getRequest().getHeaders().containsKey("X-User-Uid"),
                "X-User-Uid должен быть очищён на входе"
        );
    }

    //not work
    @Test
    void testValidBearerTokenAddsUidAndSecurityContext() throws Exception {
        // 1) Строим запрос
        var request = MockServerHttpRequest.get("/secured")
                .header("Authorization", "Bearer validToken123")
                .header("X-User-Uid", "someOldUid")
                .build();
        var originalExchange = MockServerWebExchange.from(request);

        // 2) Мокаем FirebaseAuth.getInstance()
        var authStatic = mockStatic(FirebaseAuth.class);
        try {
            FirebaseAuth authInstance = mock(FirebaseAuth.class);
            authStatic.when(FirebaseAuth::getInstance).thenReturn(authInstance);

            // 3) «Успешный» FirebaseToken
            FirebaseToken tokenMock = mock(FirebaseToken.class);
            when(tokenMock.getUid()).thenReturn("NEW-UID-XYZ");
            // Используем doReturn, чтобы Mockito перехватил вызов даже несмотря на throws
            doReturn(tokenMock).when(authInstance).verifyIdToken("validToken123");

            // 4) Готовим цепочку: сохраняем Exchange и затем извлекаем SecurityContext
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            AtomicReference<SecurityContext> capturedSecCtx = new AtomicReference<>();

            WebFilterChain chain = exchangeParam -> Mono.deferContextual(ctxView -> {
                // Сохраняем мутированный Exchange
                capturedExchange.set(exchangeParam);
                // Извлекаем созданный фильтром SecurityContext
                SecurityContext secCtx = ReactiveSecurityContextHolder.getContext().block();
                capturedSecCtx.set(secCtx);
                return Mono.empty();
            });

            // 5) Запускаем фильтр через StepVerifier, чтобы дождаться полного завершения реактивного потока
            StepVerifier.create(filter.filter(originalExchange, chain))
                    .verifyComplete();

            // 6) Проверяем, что chain.filter получил мутированный Exchange
            var ePassed = capturedExchange.get();
            assertNotNull(ePassed, "chain.filter должен получить mutatedExchange");

            // а) «Старый» X-User-Uid удалён:
            assertFalse(
                    ePassed.getRequest().getHeaders().containsKey("X-User-Uid"),
                    "Старый X-User-Uid (someOldUid) должен быть удалён"
            );
            // б) Новый X-User-Uid установлен в "NEW-UID-XYZ":
            assertEquals(
                    Collections.singletonList("NEW-UID-XYZ"),
                    ePassed.getRequest().getHeaders().get("X-User-Uid"),
                    "Должен быть добавлен новый X-User-Uid из FirebaseToken"
            );

            // 7) Проверяем SecurityContext, полученный внутри chain.filter
            SecurityContext secCtx = capturedSecCtx.get();
            assertNotNull(secCtx, "В chain.filter должен быть установлен SecurityContext");
            var auth = secCtx.getAuthentication();
            assertNotNull(auth, "Authentication не должен быть null");
            assertEquals("NEW-UID-XYZ", auth.getPrincipal(), "Principal должен совпадать с UID");
            assertTrue(
                    auth instanceof UsernamePasswordAuthenticationToken,
                    "Authentication должен быть типа UsernamePasswordAuthenticationToken"
            );
            assertTrue(auth.getAuthorities().isEmpty(), "Authorities должны быть пустыми");

        } finally {
            authStatic.close();
        }
    }




    @Test
    void testInvalidBearerTokenLeadsToUnauthorized() throws Exception {
        var request = MockServerHttpRequest.get("/secured")
                .header("Authorization", "Bearer invalidTokenXYZ")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var authStatic = mockStatic(FirebaseAuth.class);
        try {
            var authInstance = mock(FirebaseAuth.class);
            authStatic.when(FirebaseAuth::getInstance).thenReturn(authInstance);

            doThrow(new RuntimeException("invalid-token"))
                    .when(authInstance).verifyIdToken(anyString());

            AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
            WebFilterChain chain = e -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertFalse(chainCalled.get(), "chain.filter не должен быть вызван при невалидном токене");

            var response = exchange.getResponse();
            assertEquals(401, response.getStatusCode().value(), "Должен быть статус 401");
            assertTrue(response.isCommitted(), "Response должен быть committed (setComplete())");

        } finally {
            authStatic.close();
        }
    }
}
