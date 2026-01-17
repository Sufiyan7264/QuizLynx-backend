package com.projectJava.quizApp.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerScheme = "bearerAuth";
        final String cookieScheme = "cookieAuth";

        return new OpenAPI()
                .info(new Info().title("Quiz API").version("1.0"))
                // Indicate that either scheme may satisfy security for endpoints
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme).addList(cookieScheme))
                .components(new Components()
                        // Bearer (Authorization: Bearer <token>)
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                        // Cookie (cookie name JWT_TOKEN)
                        .addSecuritySchemes(cookieScheme, new SecurityScheme()
                                .name("JWT_TOKEN")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                        )
                );
    }
}
