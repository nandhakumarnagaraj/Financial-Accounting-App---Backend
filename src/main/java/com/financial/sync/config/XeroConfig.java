package com.financial.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class XeroConfig {
    
    @Bean
     WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
