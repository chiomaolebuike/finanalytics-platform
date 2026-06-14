package com.finanalytics.finanalytics_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Processed at application start up before any bean is created
// Should not be placed into a subpackage as it will cause component scan to miss sibling packages

@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class FinAnalyticsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinAnalyticsPlatformApplication.class, args);
    }
}