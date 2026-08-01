package com.pablomusaber.watson.shared.routing;

import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.AutonomyProperties;
import com.embabel.agent.api.common.ranking.Ranker;
import com.embabel.agent.api.common.ranking.Ranking;
import com.embabel.agent.api.common.ranking.Rankings;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.pablomusaber.watson.shared.channel.ChannelReply;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.watson.WatsonAgent;
import com.pablomusaber.watson.watson.WatsonReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Picks which @Agent handles an utterance using Embabel's own Ranker + Autonomy.runAgent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRouter {

    private static final String WATSON_AGENT_NAME = WatsonAgent.class.getSimpleName();

    private final Ranker ranker;
    private final Autonomy autonomy;
    private final AutonomyProperties autonomyProperties;
    private final AgentPlatform platform;

    private final AtomicReference<String> lastAgentName = new AtomicReference<>();

    public ChannelReply route(Utterance u) {
        try {
            List<Agent> agents = platform.agents();
            Agent chosen = pickAgent(agents, u.text());
            lastAgentName.set(chosen.getName());

            Object output = autonomy.runAgent(u, ProcessOptions.DEFAULT, chosen).getOutput();
            if (output instanceof ChannelReply reply) {
                return reply;
            }
            log.warn("agent {} produced no ChannelReply for utterance: {}", chosen.getName(), u.text());
        } catch (Exception e) {
            log.error("routing/execution failed for utterance: {}", u.text(), e);
        }
        return fallbackToWatson(u);
    }

    private Agent pickAgent(List<Agent> agents, String text) {
        Rankings<Agent> rankings = ranker.rank("agent", decorate(text), agents);
        List<Ranking<Agent>> ranked = rankings.rankings();
        if (!ranked.isEmpty()) {
            Ranking<Agent> top = ranked.get(0);
            if (top.getScore() >= autonomyProperties.getAgentConfidenceCutOff()) {
                return top.getMatch();
            }
        }
        return findWatson(agents);
    }

    private String decorate(String text) {
        String last = lastAgentName.get();
        return last == null ? text : "Continuing a conversation with the " + last + " agent. New message: " + text;
    }

    private Agent findWatson(List<Agent> agents) {
        return agents.stream()
                .filter(a -> WATSON_AGENT_NAME.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("WatsonAgent not registered on the platform"));
    }

    @SuppressWarnings("rawtypes")
    private ChannelReply fallbackToWatson(Utterance u) {
        Object result = AgentInvocation.builder(platform).build((Class) WatsonReply.class).invoke(u);
        if (result instanceof ChannelReply reply) {
            lastAgentName.set(WATSON_AGENT_NAME);
            return reply;
        }
        throw new IllegalStateException("Watson fallback failed to produce a ChannelReply for: " + u.text());
    }
}
