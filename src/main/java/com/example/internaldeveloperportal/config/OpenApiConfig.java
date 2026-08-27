package com.example.internaldeveloperportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 document definition, including the bearer-token security scheme
 * so Swagger UI can call protected endpoints with an issued JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI portalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Internal Developer Portal API")
                .description("Service catalogue for teams, projects, environments and deployments. "
                    + "Obtain a token from POST /api/auth/login and send it as "
                    + "'Authorization: Bearer <token>'.")
                .version("v1")
                .contact(new Contact().name("Platform Engineering"))
                .license(new License().name("Apache-2.0")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
