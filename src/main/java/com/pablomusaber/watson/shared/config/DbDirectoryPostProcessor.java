package com.pablomusaber.watson.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Must create the SQLite db directory before the DataSource bean is created (schema.sql
// runs on DataSource creation), which is too late for a @PostConstruct bean — Spring
// gives no ordering guarantee that it runs before DataSourceAutoConfiguration. Running
// as an EnvironmentPostProcessor guarantees this happens during environment prep, well
// before any bean is created.
@Slf4j
public class DbDirectoryPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbPath = environment.getProperty("listener.db-path");
        if (dbPath == null) {
            return;
        }
        try {
            Path parent = Paths.get(dbPath).getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("created db directory: {}", parent);
            }
        } catch (Exception e) {
            log.warn("failed to create db directory for {}: {}", dbPath, e.getMessage());
        }
    }
}
