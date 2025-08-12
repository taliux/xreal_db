package com.xreal.db.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Xreal FAQ Management API")
                .description("FAQ and Tag management service with MySQL and Elasticsearch integration")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Xreal Support")
                    .url("https://xreal.com")
                    .email("support@xreal.com")));
    }
}