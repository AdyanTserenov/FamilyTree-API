package com.project.familytree.auth;

import com.project.familytree.auth.config.SecurityConfig;
import com.project.familytree.auth.config.SwaggerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Family Tree Authentication Starter.
 * Enables security, JWT, and Swagger when the starter is included.
 */
@Configuration
@ConditionalOnClass(SecurityConfig.class)
@Import({SecurityConfig.class, SwaggerConfig.class})
public class AuthAutoConfiguration {
}