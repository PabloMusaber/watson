package com.pablomusaber.watson;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "GEMINI_API_KEY=context-load-only")
class WatsonApplicationTests {

	@Test
	void contextLoads() {
	}

}
