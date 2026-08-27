package com.example.internaldeveloperportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.bootstrap.*} configuration namespace. */
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

    /** Username of the seeded administrator account. */
    private String adminUsername = "admin";

    /** Password of the seeded administrator account. */
    private String adminPassword = "";

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
