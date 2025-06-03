package com.practice.review.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public";
    }

    @GetMapping("/private")
    public String privateEndpoint(Authentication auth) {
        return "Hello user " + auth.getName();
    }
}