package com.practice.review.security;

import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

@Component
public class FirebaseAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Удаляем X-User-Uid на входе
        ServerWebExchange cleanedExchange = exchange.mutate()
                .request(r -> r.headers(headers -> headers.remove("X-User-Uid")))
                .build();

        String authHeader = cleanedExchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Нет заголовка Authorization или он не начинается с Bearer → пропускаем далее без аутентификации");
            return chain.filter(cleanedExchange);
        }

        String idToken = authHeader.substring(7).trim();
        log.debug("Найден Authorization: Bearer <{}...>. Начинаем verifyIdToken", idToken.length() > 10 ? idToken.substring(0, 10) : idToken);

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(firebaseToken -> {
                    String firebaseUid = firebaseToken.getUid();

                    UUID userUuid = UUID.nameUUIDFromBytes(firebaseUid.getBytes(StandardCharsets.UTF_8));
                    String uuidString = userUuid.toString();

                    log.info("verifyIdToken успешен. Firebase UID={} → UUID={}", firebaseUid, uuidString);
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Uid", uuidString)
                                    .build())
                            .build();

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            uuidString,
                            null,
                            Collections.emptyList()
                    );
                    SecurityContextImpl securityContext = new SecurityContextImpl(auth);

                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                })
                .onErrorResume(e -> {
                    log.warn("Ошибка при verifyIdToken: {}", e.getMessage());
                    log.debug("Stacktrace:", e);
                    cleanedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return cleanedExchange.getResponse().setComplete();
                });
    }
}
