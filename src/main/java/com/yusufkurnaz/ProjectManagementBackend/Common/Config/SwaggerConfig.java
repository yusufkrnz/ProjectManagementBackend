package com.yusufkurnaz.ProjectManagementBackend.Common.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Management Backend API")
                        .version("1.0.0")
                        .description("Project Management System Backend API Documentation")
                        .contact(new Contact()
                                .name("Yusuf Kurnaz")
                                .email("yusuf@example.com")));
    }
}
