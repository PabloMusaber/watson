package com.pablomusaber.watson;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"GOOGLE_API_KEY=context-load-only",
		"OPENWA_BASE_URL=http://localhost:2785",
		"OPENWA_API_KEY=context-load-only",
		"OPENWA_SESSION_ID=context-load-only",
		"OPENWA_WEBHOOK_SECRET=context-load-only",
		"OPENWA_REPLY_CHAT_ID=context-load-only",
		"WHATSAPP_ALLOWED_IDS=context-load-only",
		"BRAVE_API_KEY=context-load-only",
		"TELEGRAM_BOT_TOKEN=context-load-only",
		"TELEGRAM_CHAT_ID=context-load-only",
		"TELEGRAM_REPLY_CHAT_ID=context-load-only",
		"TELEGRAM_ALLOWED_IDS=context-load-only",
		"TELEGRAM_WEBHOOK_SECRET=context-load-only"
})
class WatsonApplicationTests {

	@Test
	void contextLoads() {
	}

}
