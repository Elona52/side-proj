package com.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AuctionHub API")
                        .version("1.0.0")
                        .description("경매 플랫폼 API 문서입니다. 온비드 공공 경매와 개인 경매를 통합한 플랫폼입니다.")
                        .contact(new Contact()
                                .name("AuctionHub Team")
                                .email("contact@auctionhub.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:5555")
                                .description("개발 서버")
                ));
    }
}

