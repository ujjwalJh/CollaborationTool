package com.collaborationTool.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.collaborationTool.service.PresenceService;
import com.collaborationTool.ws.PresenceMessage;

/**
 * Receives presence messages from clients (via /app/presence).
 * Tracks session -> doc membership (so disconnects can be handled).
 */
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
        // track membership for join/leave
        if ("join".equals(msg.getType())) {
            if (sessionId != null) presenceService.registerSessionDoc(sessionId, msg.getDocId());
        } else if ("leave".equals(msg.getType())) {
            if (sessionId != null) presenceService.unregisterSessionDoc(sessionId, msg.getDocId());
        }

        // attach client session id if not present
        if (msg.getClientId() == null && sessionId != null) msg.setClientId(sessionId);
        System.out.println("RECEIVED Presence message = " + msg);
        // publish (local + redis)
        presenceService.publish(msg);
    }
}
