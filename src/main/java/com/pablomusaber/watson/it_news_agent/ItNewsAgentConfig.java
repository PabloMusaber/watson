package com.pablomusaber.watson.it_news_agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ItNewsAgentConfig {

    @Bean("braveRestClient")
    public RestClient braveRestClient(@Value("${news-agent.brave.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl("https://api.search.brave.com/res/v1")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("X-Subscription-Token", apiKey)
                .build();
    }
}
