package com.example.internaldeveloperportal.config;

import com.example.internaldeveloperportal.domain.PortalUser;
import com.example.internaldeveloperportal.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the administrator account on first start so the portal is usable
 * immediately after a fresh deployment. Does nothing if the user exists or
 * no bootstrap password was configured.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSeeder.class);

    private final PortalUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    public AdminSeeder(PortalUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       BootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.getAdminUsername();
        String password = properties.getAdminPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOG.info("Admin bootstrap skipped: no credentials configured");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            LOG.info("Admin bootstrap skipped: user '{}' already exists", username);
            return;
        }

        PortalUser admin = new PortalUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);
        LOG.info("Seeded administrator account '{}'", username);
    }
}
