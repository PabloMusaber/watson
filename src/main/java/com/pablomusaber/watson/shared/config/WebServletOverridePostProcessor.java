package com.pablomusaber.watson.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

// Embabel's ShellEnvironmentPostProcessor runs at Integer.MIN_VALUE + 10 and forces
// spring.main.web-application-type=none and spring.shell.interactive.enabled=true via
// addFirst — which also beats @TestPropertySource, since that's applied even earlier
// in the SpringApplication bootstrap. Running at MIN_VALUE + 11 (one step later) lets
// us call addFirst again and win both overrides: servlet support for our WebSocket
// server, and (test JVM only, see pom.xml surefire config) a non-interactive shell so
// `mvn test` doesn't hang reading from stdin.
public class WebServletOverridePostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.main.web-application-type", "servlet");
        if (Boolean.getBoolean("watson.test")) {
            overrides.put("spring.shell.interactive.enabled", "false");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("webServletOverride", overrides));
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 11;
    }
}
