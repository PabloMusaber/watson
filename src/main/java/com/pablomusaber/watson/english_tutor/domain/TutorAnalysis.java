package com.pablomusaber.watson.english_tutor.domain;

import com.pablomusaber.watson.shared.channel.ChannelReply;

import java.util.List;

public record TutorAnalysis(
        List<Correction> corrections,
        List<String> repeatedWords,
        String responseText
) implements ChannelReply {

    @Override
    public String text() {
        return responseText;
    }
}
