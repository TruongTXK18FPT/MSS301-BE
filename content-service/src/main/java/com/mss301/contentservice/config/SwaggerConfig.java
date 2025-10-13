package com.mss301.contentservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Content Service API")
                        .description("APIs for managing learning contents")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MSS301 Team")
                                .email("support@mss301.com")
                                .url("https://mss301.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(new Server().url("http://localhost:8085").description("Development server")));
    }
}
