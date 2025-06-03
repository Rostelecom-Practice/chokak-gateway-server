package com.practice.review.security;

import com.google.firebase.auth.FirebaseAuth;
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

import java.util.Collections;

@Component
public class FirebaseAuthenticationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange cleanedExchange = exchange.mutate()
                .request(r -> r.headers(headers -> headers.remove("X-User-Uid")))
                .build();

        String authHeader = cleanedExchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(cleanedExchange);
        }

        String idToken = authHeader.substring(7);

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(firebaseToken -> {
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Uid", firebaseToken.getUid())
                                    .build())
                            .build();

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            firebaseToken.getUid(),
                            null,
                            Collections.emptyList()
                    );

                    SecurityContextImpl securityContext = new SecurityContextImpl(auth);

                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                })
                .onErrorResume(e -> {
                    cleanedExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return cleanedExchange.getResponse().setComplete();
                });
    }
}
