package com.enterprise.idp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Internal Developer Portal application.
 */
@SpringBootApplication
@EnableJpaAuditing
public class InternalDeveloperPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternalDeveloperPortalApplication.class, args);
    }
}
