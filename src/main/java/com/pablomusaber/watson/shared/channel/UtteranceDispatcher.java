package com.pablomusaber.watson.shared.channel;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.pablomusaber.watson.shared.PersonaProfile;
import com.pablomusaber.watson.shared.tui.TuiPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Single-thread, bounded-queue dispatcher: utterances are processed in arrival order.
 * Queue is capped at 100; overflow drops with a warning to keep WebSocket reads non-blocking.
 * The goal class is supplied by the active PersonaProfile bean. Reply delivery is generic:
 * any goal DTO that implements ChannelReply gets routed back to the channel it arrived on,
 * so agents themselves never need to know about Channel/transport.
 */
@Slf4j
@Service
public class UtteranceDispatcher {
    private static final int QUEUE_CAPACITY = 100;

    private final ThreadPoolExecutor exec;
    private final AgentPlatform platform;
    private final Class<?> goalClass;
    private final ChannelRegistry channels;
    private final TuiPrinter tui;

    public UtteranceDispatcher(AgentPlatform platform, PersonaProfile target, ChannelRegistry channels, TuiPrinter tui) {
        this.platform = platform;
        this.goalClass = target.goalClass();
        this.channels = channels;
        this.tui = tui;
        this.exec = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "utterance-dispatcher");
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> log.warn("[buffer-full] dropping utterance"));
    }

    public void dispatch(Utterance u) {
        exec.submit(() -> handle(u));
    }

    @SuppressWarnings("rawtypes")
    private void handle(Utterance u) {
        tui.heard(u, null);
        log.info("dispatching utterance: {} ({} via {})", u.text(), u.source(), u.channelId());
        try {
            Object result = AgentInvocation
                    .builder(platform)
                    .build((Class) goalClass)
                    .invoke(u);
            deliver(u, result);
        } catch (Exception e) {
            log.error("agent run failed for utterance: {}", u.text(), e);
        }
    }

    private void deliver(Utterance u, Object result) {
        if (!(result instanceof ChannelReply reply)) {
            log.warn("goal result does not implement ChannelReply, cannot deliver to channel: {}", result);
            return;
        }
        Channel channel = channels.get(u.channelId());
        channel.reply(reply.text());
        if (u.source() == Source.MIC && channel.supportsAudioOut()) {
            channel.speak(reply.text());
        }
    }
}
