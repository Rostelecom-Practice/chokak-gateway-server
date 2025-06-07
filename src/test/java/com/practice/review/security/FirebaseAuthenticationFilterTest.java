package com.practice.review.security;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

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
