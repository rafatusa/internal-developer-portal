package com.enterprise.idp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger UI configuration with JWT bearer authentication.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Internal Developer Portal API",
        version = "1.0.0",
        description = "Enterprise Internal Developer Portal REST API — manages Projects, Teams, "
            + "Environments, and Deployments with JWT-based authentication.",
        contact = @Contact(
            name = "Platform Engineering Team",
            email = "platform@enterprise.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/", description = "Current server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Provide the JWT token obtained from POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
