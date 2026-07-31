package com.pablomusaber.watson;

import com.pablomusaber.watson.shared.openwa.OpenWaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenWaProperties.class)
public class WatsonApplication {

	public static void main(String[] args) {
		// Avoids JDK HttpClient reusing pooled connections open-wa already closed
		System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection");
		SpringApplication.run(WatsonApplication.class, args);
	}

}
