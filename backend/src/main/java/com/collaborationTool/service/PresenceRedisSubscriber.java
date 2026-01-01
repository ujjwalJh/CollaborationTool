package com.collaborationTool.service;

import org.springframework.stereotype.Component;

import com.collaborationTool.ws.PresenceMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PresenceRedisSubscriber {

    private final PresenceService presenceService;
    private final ObjectMapper objectMapper;

    public PresenceRedisSubscriber(PresenceService presenceService, ObjectMapper objectMapper) {
        this.presenceService = presenceService;
        this.objectMapper = objectMapper;
    }

    
    public void handleMessage(String message) {
        try {
            PresenceMessage msg = objectMapper.readValue(message, PresenceMessage.class);
            
            presenceService.publishLocal(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
