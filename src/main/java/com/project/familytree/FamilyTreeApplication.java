package com.project.familytree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class FamilyTreeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FamilyTreeApplication.class, args);
    }

}
