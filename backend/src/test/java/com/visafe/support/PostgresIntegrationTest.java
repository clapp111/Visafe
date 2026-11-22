package com.visafe.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 PostgreSQL 스키마(Flyway V1부터 데모 시드까지)로 실행하는 통합 테스트. 각 테스트는 롤백된다.
 * <p>
 * 환경 변수 IT_DB_URL이 있을 때만 실행한다. (CI에서는 건너뜀)
 * IT_DB_URL=jdbc:postgresql://localhost:5432/visafe, IT_DB_USERNAME·IT_DB_PASSWORD 기본값 visafe
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "IT_DB_URL", matches = "jdbc:postgresql:.+")
@TestPropertySource(
        properties = {
            "spring.datasource.url=${IT_DB_URL}",
            "spring.datasource.username=${IT_DB_USERNAME:visafe}",
            "spring.datasource.password=${IT_DB_PASSWORD:visafe}",
            "spring.datasource.driver-class-name=org.postgresql.Driver",
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
public @interface PostgresIntegrationTest {}
