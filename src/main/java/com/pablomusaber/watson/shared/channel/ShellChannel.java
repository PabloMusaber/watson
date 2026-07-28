package com.pablomusaber.watson.shared.channel;

import com.pablomusaber.watson.shared.tui.TuiPrinter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShellChannel implements Channel {

    private final TuiPrinter tui;

    @Override
    public String id() {
        return "shell";
    }

    @Override
    public boolean supportsAudioOut() {
        return false;
    }

    @Override
    public void reply(String text) {
        tui.respond("watson", text);
    }
}
