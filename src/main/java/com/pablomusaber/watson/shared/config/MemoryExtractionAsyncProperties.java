package com.pablomusaber.watson.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.async.memory-extraction")
public class MemoryExtractionAsyncProperties {
    private int corePoolSize = 2;
    private int maxPoolSize = 5;
    private int queueCapacity = 25;
}
