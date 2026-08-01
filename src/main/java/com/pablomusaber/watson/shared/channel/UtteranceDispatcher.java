package com.pablomusaber.watson.shared.channel;

import com.pablomusaber.watson.shared.routing.AgentRouter;
import com.pablomusaber.watson.shared.tui.TuiPrinter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Single-thread, bounded-queue dispatcher: utterances are processed in arrival order.
 * Queue is capped at 100; overflow drops with a warning to keep WebSocket reads non-blocking.
 * Which agent handles an utterance is decided by AgentRouter. Reply delivery is generic:
 * any goal DTO that implements ChannelReply gets routed back to the channel it arrived on,
 * so agents themselves never need to know about Channel/transport.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UtteranceDispatcher {
    private static final int QUEUE_CAPACITY = 100;

    private final AgentRouter router;
    private final ChannelRegistry channels;
    private final TuiPrinter tui;

    private final ThreadPoolExecutor exec = new ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread t = new Thread(r, "utterance-dispatcher");
                t.setDaemon(true);
                return t;
            },
            (r, executor) -> log.warn("[buffer-full] dropping utterance"));

    public void dispatch(Utterance u) {
        exec.submit(() -> handle(u));
    }

    private void handle(Utterance u) {
        tui.heard(u, null);
        log.info("dispatching utterance: {} ({} via {})", u.text(), u.source(), u.channelId());
        try {
            ChannelReply reply = router.route(u);
            deliver(u, reply);
        } catch (Exception e) {
            log.error("agent run failed for utterance: {}", u.text(), e);
        }
    }

    private void deliver(Utterance u, ChannelReply reply) {
        Channel channel = channels.get(u.channelId());
        channel.reply(reply.text());
        if (u.source() == Source.MIC && channel.supportsAudioOut()) {
            channel.speak(reply.text());
        }
    }
}
