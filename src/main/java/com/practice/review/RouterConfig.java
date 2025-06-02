package com.practice.review;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Configuration
@ComponentScan
public class RouterConfig {

    @Value("${services.core.url}")
    private String coreServiceUrl;

    @Value("${services.storage.url}")
    private String storageServiceUrl;



    public @Bean RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("core-service", r -> r
                        .path("/api/**")
                            .filters(f->f
                                    .stripPrefix(1)
                                    .addRequestHeader("X-Gateway", "chokak-gateway")
                            )
                            .uri(coreServiceUrl))
                .route("cloud-storage", r -> r
                        .path("/uploads/**")
                            .filters(f->f
                                    .stripPrefix(1)
                                    .addRequestHeader("X-Gateway", "chokak-gateway"))
                            .uri(storageServiceUrl)
                )
                .build();
    }

}
