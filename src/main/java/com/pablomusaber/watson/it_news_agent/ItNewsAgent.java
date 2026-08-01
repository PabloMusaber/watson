package com.pablomusaber.watson.it_news_agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.pablomusaber.watson.shared.PromptLoader;
import com.pablomusaber.watson.shared.channel.Utterance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.Map;

@Slf4j
@Agent(description = "Fetches a curated digest of today's IT news (AI/LLM, developer tools, cloud/infrastructure) "
        + "and sends it to Telegram. Use only when the user explicitly asks for tech/IT news or a news digest.")
@RequiredArgsConstructor
public class ItNewsAgent {

    private final BraveSearchTool braveSearch;
    private final TelegramService telegramService;
    private final PromptLoader promptLoader;

    @Value("classpath:it-news-agent/prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:it-news-agent/prompts/news-digest.st")
    private Resource newsDigestPrompt;

    @AchievesGoal(description = "Daily IT news digest compiled and sent to Telegram.")
    @Action
    public NewsDigest generateAndSend(Utterance u, Ai ai) {
        log.info("ItNewsAgent action started — running 3 Brave searches");

        String aiResults = braveSearch.search("AI LLM model release breakthrough news today");
        String devResults = braveSearch.search("developer tools frameworks new release news today");
        String cloudResults = braveSearch.search("cloud AWS GCP Azure Kubernetes platform news today");

        log.info("Searches complete — generating digest with LLM");

        String prompt = promptLoader.render(newsDigestPrompt, Map.of(
                "systemPrompt", promptLoader.load(systemPrompt),
                "aiLlmResults", aiResults,
                "devToolsResults", devResults,
                "cloudInfraResults", cloudResults));

        String digest = ai.withDefaultLlm().generateText(prompt);
        log.info("Digest generated ({} chars) — sending to Telegram", digest.length());
        telegramService.sendMessage(digest);
        log.info("IT news digest sent.");

        return new NewsDigest(digest);
    }
}
