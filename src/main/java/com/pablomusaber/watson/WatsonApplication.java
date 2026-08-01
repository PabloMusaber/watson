package com.pablomusaber.watson;

import com.pablomusaber.watson.shared.config.MemoryExtractionAsyncProperties;
import com.pablomusaber.watson.shared.openwa.OpenWaProperties;
import com.pablomusaber.watson.shared.telegram.TelegramProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenWaProperties.class, MemoryExtractionAsyncProperties.class, TelegramProperties.class})
public class WatsonApplication {

	public static void main(String[] args) {
		// Avoids JDK HttpClient reusing pooled connections open-wa already closed
		System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection");
		SpringApplication.run(WatsonApplication.class, args);
	}

}
