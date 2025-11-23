package com.collaborationTool.ws;

import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.collaborationTool.service.PresenceService;

/**
 * Listens for WebSocket session disconnects and emits leave presence for tracked docs.
 */
@Component
public class SessionPresenceListener {

    private final PresenceService presenceService;

    public SessionPresenceListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();

        // all docs this session participated in
        Set<String> docs = presenceService.getDocsForSession(sessionId);
        if (docs == null || docs.isEmpty()) return;

        // send leave for each doc
        for (String docId : docs) {
            PresenceMessage leave = new PresenceMessage();
            leave.setType("leave");
            leave.setDocId(docId);
            // set clientId as session for cleanup
            leave.setClientId(sessionId);
            // user info may not be available here, but we broadcast leave with clientId
            presenceService.publish(leave);
        }

        // cleanup
        for (String docId : docs) {
            presenceService.unregisterSessionDoc(sessionId, docId);
        }
    }
}
