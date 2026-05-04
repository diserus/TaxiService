package com.taxi.notification;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
// WorkerPool в каждом тестовом контексте поллит общую Postgres-testcontainer
// БД. Закрываем контекст после класса, чтобы воркеры одного теста не крали
// задачи у воркеров другого.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    static final String JWT_SECRET = "test-secret-key-must-be-at-least-256-bits-long-so-here-is-some-padding";

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taxi")
            .withUsername("taxi_user")
            .withPassword("taxi_password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("notification.worker.pool-size", () -> "4");
        registry.add("notification.worker.poll-interval-ms", () -> "50");
        registry.add("notification.worker.max-attempts", () -> "3");
    }
}
