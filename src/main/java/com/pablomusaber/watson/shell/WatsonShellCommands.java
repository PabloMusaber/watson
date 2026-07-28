package com.pablomusaber.watson.shell;

import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@RequiredArgsConstructor
public class WatsonShellCommands {

    private final UtteranceDispatcher dispatcher;

    @ShellMethod(key = "say", value = "Send text to Watson as if typed.")
    public String say(@ShellOption(arity = Integer.MAX_VALUE) String[] words) {
        String text = String.join(" ", words);
        dispatcher.dispatch(Utterance.fromTyped(text, "shell"));
        return "[sent]";
    }
}
