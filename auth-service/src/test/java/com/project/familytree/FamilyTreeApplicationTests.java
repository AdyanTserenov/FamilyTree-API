package com.project.familytree;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads successfully.
 *
 * @ActiveProfiles("test") activates src/test/resources/application.properties
 * which uses H2 in-memory DB instead of PostgreSQL.
 *
 * JavaMailSender is mocked so the context doesn't require a real SMTP server.
 */
@SpringBootTest
@ActiveProfiles("test")
class FamilyTreeApplicationTests {

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
        // If the context starts without exceptions, the test passes.
    }
}
