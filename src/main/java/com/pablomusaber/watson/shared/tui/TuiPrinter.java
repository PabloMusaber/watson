package com.pablomusaber.watson.shared.tui;

import com.pablomusaber.watson.shared.channel.Utterance;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class TuiPrinter {

    private final LineReader reader;

    public TuiPrinter(@Autowired(required = false) @Lazy LineReader reader) {
        this.reader = reader;
    }

    public void printAbove(String line) {
        if (reader != null) {
            reader.printAbove(line);
        } else {
            System.out.println(line);
        }
    }

    public void heard(Utterance u, String suffix) {
        String tail = (suffix == null || suffix.isBlank()) ? "" : " (" + suffix + ")";
        printAbove("[heard] " + u.text() + tail);
    }

    public void respond(String persona, String text) {
        printAbove("[" + persona + "] " + text);
    }
}
