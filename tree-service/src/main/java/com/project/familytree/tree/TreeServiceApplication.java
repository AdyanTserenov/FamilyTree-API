package com.project.familytree.tree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
    basePackages = {"com.project.familytree.auth", "com.project.familytree.tree"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.project\\.familytree\\.auth\\.exceptions\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.project\\.familytree\\.auth\\.config\\.SwaggerConfig"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.project\\.familytree\\.auth\\.controllers\\..*"
        )
    }
)
@EntityScan(basePackages = {"com.project.familytree.auth.models", "com.project.familytree.tree.models"})
@EnableJpaRepositories(basePackages = {"com.project.familytree.auth.repositories", "com.project.familytree.tree.repositories"})
public class TreeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TreeServiceApplication.class, args);
    }
}