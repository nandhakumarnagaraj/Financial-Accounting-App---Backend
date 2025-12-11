package com.financial.sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "zerodha")
public class ZerodhaProperties {
    private String apiKey;
    private String apiSecret;
    private String redirectUri;
    private String loginUrl;
    private String apiUrl;
}