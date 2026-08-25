package com.enterprise.idp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    // HS512 requires >= 64 bytes — 80 alphanumeric chars, no special characters
    "app.jwt.secret=contextloadsmoketestjwtsecretkeyforhs512algorithmexactlyeightycharacterslong1234",
    "app.jwt.expiration-ms=86400000",
    "app.jwt.refresh-expiration-ms=604800000"
})
@DisplayName("Application Context Loads")
class InternalDeveloperPortalApplicationTest {

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // If the context fails to load, this test fails automatically.
    }
}
