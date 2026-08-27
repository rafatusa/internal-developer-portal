package com.example.internaldeveloperportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Internal Developer Portal.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class InternalDeveloperPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternalDeveloperPortalApplication.class, args);
    }
}
