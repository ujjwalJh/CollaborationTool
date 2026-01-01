package com.collaborationTool.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.collaborationTool.service.PresenceService;
import com.collaborationTool.ws.PresenceMessage;

@Controller
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/presence")
    public void onPresence(PresenceMessage msg, SimpMessageHeaderAccessor headers) {
        if (msg == null || msg.getDocId() == null || msg.getType() == null) return;

        String sessionId = headers.getSessionId();
        if (msg.getClientId() == null && sessionId != null) {
            msg.setClientId(sessionId);
        }
        presenceService.publish(msg);

        System.out.println("REDIS PUBLISHED presence = " + msg);
    }
}
