package com.pablomusaber.watson.shared.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChannelRegistry {

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public ChannelRegistry(List<Channel> registered) {
        registered.forEach(c -> channels.put(c.id(), c));
    }

    public Channel get(String id) {
        Channel channel = channels.get(id);
        if (channel == null) {
            throw new IllegalStateException("No channel registered for id: " + id);
        }
        return channel;
    }
}
