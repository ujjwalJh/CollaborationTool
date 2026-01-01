package com.collaborationTool.ws;

import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.collaborationTool.service.PresenceService;

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

        
        Set<String> docs = presenceService.getDocsForSession(sessionId);
        if (docs == null || docs.isEmpty()) return;

        
        for (String docId : docs) {
            PresenceMessage leave = new PresenceMessage();
            leave.setType("leave");
            leave.setDocId(docId);

            leave.setClientId(sessionId);
            
            presenceService.publish(leave);
        }


        for (String docId : docs) {
            presenceService.unregisterSessionDoc(sessionId, docId);
        }
    }
}
