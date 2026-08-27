package com.example.internaldeveloperportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.internaldeveloperportal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InternalDeveloperPortalApplicationTests {

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
        assertThat(jwtService).isNotNull();
    }
}
