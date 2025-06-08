package com.practice.review.config;

import com.practice.review.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/api/review/submit",
                                "/api/review/reply/**",
                                "/api/review/react",
                                "/api/users/me/reviews"
                        ).authenticated()
                        .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .pathMatchers( "/uploads/**").authenticated()
                        .anyExchange().permitAll()
                )
                .addFilterAt(new FirebaseAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}

