package com.practice.review.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {


    @GetMapping("/public")
    public Mono<String> publicEndpoint() {
        return Mono.just("Public");
    }

    @GetMapping("/private")
    public Mono<String> privateEndpoint(Authentication auth) {
        return Mono.just("Hello user " + auth.getName());
    }
}