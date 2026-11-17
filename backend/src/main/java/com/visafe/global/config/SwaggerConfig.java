package com.visafe.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final List<Server> SERVER_LIST = List.of(createServer("http://localhost:8080", "Local Server"));

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .name("JWT Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .info(apiInfo())
                .servers(SERVER_LIST);
    }

    private Info apiInfo() {
        return new Info()
                .title("Visafe API Documentation")
                .description("외국인 근로자를 위한 근로계약서 교차검증 플랫폼 API 문서")
                .version("1.0.0");
    }

    private static Server createServer(String url, String description) {
        return new Server().url(url).description(description);
    }
}
