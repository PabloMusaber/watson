package com.pablomusaber.watson.shared;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PromptLoader {

    public String load(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt: " + resource.getDescription(), e);
        }
    }

    public String render(Resource resource, Map<String, Object> attributes) {
        String content = load(resource);
        ST template = new ST(content, '$', '$');
        attributes.forEach(template::add);
        return template.render();
    }
}
