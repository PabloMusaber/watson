package com.pablomusaber.watson.financial_agent.domain;

import com.pablomusaber.watson.shared.channel.ChannelReply;

public record FinancialResponse(String spokenResponse, String terminalOutput) implements ChannelReply {

    @Override
    public String text() {
        return terminalOutput.equals(spokenResponse)
                ? terminalOutput
                : terminalOutput + "\n\n" + spokenResponse;
    }
}
