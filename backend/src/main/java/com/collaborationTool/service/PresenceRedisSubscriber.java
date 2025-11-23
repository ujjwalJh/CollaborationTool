package com.collaborationTool.service;

import com.collaborationTool.ws.PresenceMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Called by Redis message listener adapter when a message arrives on channel "presence".
 * It simply parses JSON and re-broadcasts to local clients via PresenceService (which will not
 * re-publish back to Redis to avoid cycles).
 */
@Component
public class PresenceRedisSubscriber {

    private final PresenceService presenceService;
    private final ObjectMapper objectMapper;

    public PresenceRedisSubscriber(PresenceService presenceService, ObjectMapper objectMapper) {
        this.presenceService = presenceService;
        this.objectMapper = objectMapper;
    }

    // called reflectively by MessageListenerAdapter
    public void handleMessage(String message) {
        try {
            PresenceMessage msg = objectMapper.readValue(message, PresenceMessage.class);
            // broadcast to local clients only (do not republish to Redis)
            presenceService.publishLocal(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
