package com.practice.review.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
public class RouterConfig {

    @Value("${services.core.url}")
    private String coreServiceUrl;

    @Value("${services.storage.url}")
    private String storageServiceUrl;



    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("core-api", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Gateway", "chokak-gateway"))
                        .uri(coreServiceUrl))

                .route("core-static", r -> r
                        .path("/", "/index.html", "/assets/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "chokak-gateway"))
                        .uri(coreServiceUrl))

                .route("uploads", r -> r
                        .path("/uploads/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Gateway", "chokak-gateway"))
                        .uri(storageServiceUrl))

                .build();
    }


}
