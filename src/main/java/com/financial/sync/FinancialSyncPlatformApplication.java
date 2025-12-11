package com.financial.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinancialSyncPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialSyncPlatformApplication.class, args);
    }
}

