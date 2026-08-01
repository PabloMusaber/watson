package com.pablomusaber.watson.it_news_agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BraveSearchTool {

    private final RestClient braveRestClient;
    private final ObjectMapper mapper;

    public BraveSearchTool(@Qualifier("braveRestClient") RestClient braveRestClient,
                           ObjectMapper mapper) {
        this.braveRestClient = braveRestClient;
        this.mapper = mapper;
    }

    public String search(String query) {
        log.info("BraveSearch: querying '{}'", query);
        try {
            String response = braveRestClient.get()
                    .uri("/web/search?q={q}&count=10&freshness=pd", query)
                    .retrieve()
                    .body(String.class);

            String results = parseResults(response);
            log.info("BraveSearch: got {} chars for '{}'", results.length(), query);
            return results;
        } catch (Exception e) {
            log.warn("Brave search failed for query '{}': {}", query, e.getMessage());
            return "No results found.";
        }
    }

    private String parseResults(String json) throws Exception {
        JsonNode results = mapper.readTree(json).path("web").path("results");
        StringBuilder sb = new StringBuilder();
        for (JsonNode r : results) {
            sb.append("Title: ").append(r.path("title").asText()).append("\n");
            sb.append("URL: ").append(r.path("url").asText()).append("\n");
            sb.append("Description: ").append(r.path("description").asText()).append("\n\n");
        }
        return sb.isEmpty() ? "No results found." : sb.toString();
    }
}
