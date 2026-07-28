package com.pablomusaber.watson.watson;

import com.pablomusaber.watson.shared.PersonaProfile;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class WatsonPersonaProfile implements PersonaProfile {

    @Value("classpath:watson/prompts/system.st")
    private Resource systemPromptResource;
    private String systemPrompt = "";

    @PostConstruct
    private void loadSystemPrompt() {
        try {
            this.systemPrompt = new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load watson system prompt", e);
        }
    }

    @Override
    public String name() {
        return "watson";
    }

    @Override
    public String systemPrompt() {
        return systemPrompt;
    }

    @Override
    public String goalDescription() {
        return "Have a friendly, warm conversation and answer whatever the user asks.";
    }

    @Override
    public List<String> watchlist() {
        return List.of();
    }

    @Override
    public Class<?> goalClass() {
        return WatsonReply.class;
    }
}
