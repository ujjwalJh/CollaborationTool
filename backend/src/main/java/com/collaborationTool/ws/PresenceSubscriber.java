package com.collaborationTool.ws;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.collaborationTool.service.PresenceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PresenceSubscriber implements MessageListener {

    private final PresenceService presenceService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            PresenceMessage msg = PresenceMessage.fromJson(json);
            presenceService.publishLocal(msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
