package com.enterprise.idp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.jwt.secret=unit-test-jwt-secret-key-must-be-at-least-32-chars"
})
@DisplayName("Application Context Loads")
class InternalDeveloperPortalApplicationTest {

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // If the context fails to load, this test fails — intentionally minimal.
    }
}
