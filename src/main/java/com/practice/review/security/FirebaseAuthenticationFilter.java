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
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String idToken = authHeader.substring(7);

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .map(firebaseToken -> {
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            firebaseToken.getUid(),
                            null,
                            Collections.emptyList()
                    );
                    return new SecurityContextImpl(auth);
                })
                .flatMap(securityContext ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }
}
