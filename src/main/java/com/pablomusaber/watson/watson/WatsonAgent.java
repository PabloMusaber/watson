package com.pablomusaber.watson.watson;

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
@Agent(description = "Watson: the default, friendly and warm conversational agent.")
@RequiredArgsConstructor
public class WatsonAgent {

    private final WatsonPersonaProfile profile;
    private final PromptLoader promptLoader;

    @Value("classpath:watson/prompts/reply.st")
    private Resource replyPrompt;

    @AchievesGoal(description = "Utterance answered.")
    @Action
    public WatsonReply reply(Utterance u, Ai ai) {
        String prompt = promptLoader.render(replyPrompt, Map.of(
                "systemPrompt", profile.systemPrompt(),
                "utterance", u.text()));
        String text = ai.withDefaultLlm().generateText(prompt);
        return new WatsonReply(text);
    }
}
