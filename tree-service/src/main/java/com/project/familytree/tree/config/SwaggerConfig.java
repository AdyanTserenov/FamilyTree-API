package com.project.familytree.tree.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger/OpenAPI для tree-service.
 * Документация доступна по адресу: /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI familyTreeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Family Tree API — Tree Service")
                        .description("REST API для управления семейными деревьями, персонами, " +
                                     "медиафайлами, комментариями и уведомлениями")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Церенов А.Б.")
                                .email("abtserenov@edu.hse.ru")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен, полученный при входе в систему")));
    }
}
