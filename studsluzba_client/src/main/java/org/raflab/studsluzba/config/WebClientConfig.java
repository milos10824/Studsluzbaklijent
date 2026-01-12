package org.raflab.studsluzba.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(@Value("${app.api.baseUrl}") String baseUrl) {
        System.out.println(">>> CLIENT baseUrl = [" + baseUrl + "]");
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}

