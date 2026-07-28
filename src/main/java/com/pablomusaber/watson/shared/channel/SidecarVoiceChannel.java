package com.pablomusaber.watson.shared.channel;

import com.pablomusaber.watson.shared.net.TranscriptionWebSocketHandler;
import com.pablomusaber.watson.shared.tui.TuiPrinter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SidecarVoiceChannel implements Channel {

    private final TuiPrinter tui;
    private final TranscriptionWebSocketHandler wsHandler;

    // wsHandler is @Lazy to break the cycle: Handler -> Dispatcher -> ChannelRegistry -> this.
    public SidecarVoiceChannel(TuiPrinter tui, @Lazy TranscriptionWebSocketHandler wsHandler) {
        this.tui = tui;
        this.wsHandler = wsHandler;
    }

    @Override
    public String id() {
        return "sidecar";
    }

    @Override
    public boolean supportsAudioOut() {
        return true;
    }

    @Override
    public void reply(String text) {
        tui.respond("watson", text);
    }

    @Override
    public void speak(String text) {
        wsHandler.broadcastTts(text);
    }
}
