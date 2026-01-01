package com.collaborationTool.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.collaborationTool.ws.PresenceMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String CHANNEL_PREFIX = "presence:";

    private final ConcurrentHashMap<String, Set<String>> sessionDocs = new ConcurrentHashMap<>();

    public void publish(PresenceMessage msg) {
    try {
        publishLocal(msg);

        String json = mapper.writeValueAsString(msg);
        String channel = CHANNEL_PREFIX + msg.getDocId();
        redis.convertAndSend(channel, json);

    } catch (Exception e) {
        e.printStackTrace();
    }
}
    public void publishLocal(PresenceMessage message) {
        messaging.convertAndSend("/topic/presence." + message.getDocId(), message);
    }

    public void registerSessionDoc(String sessionId, String docId) {
        sessionDocs.computeIfAbsent(sessionId, s -> ConcurrentHashMap.newKeySet())
                   .add(docId);
    }

    public Set<String> getDocsForSession(String sessionId) {
        return sessionDocs.getOrDefault(sessionId, Collections.emptySet());
    }

    public void unregisterSessionDoc(String sessionId, String docId) {
        Set<String> docs = sessionDocs.get(sessionId);
        if (docs != null) {
            docs.remove(docId);
            if (docs.isEmpty()) {
                sessionDocs.remove(sessionId);
            }
        }
    }
}
