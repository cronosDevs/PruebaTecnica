package com.example.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(properties = "app.jwt.secret=Qd+hvCYf2SVEXdqW9rM52k/Nf0PEvl9zGCv9i33mAsi3qMF77VbOEjDdcsCma7AX")
abstract class IntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("product_db.sql"),
                    "/docker-entrypoint-initdb.d/01-product_db.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
