package com.collaborationTool.ws;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PresenceMessage implements Serializable {

    private String type;
    private String docId;
    private String user;
    private Long userId;
    private String clientId;
    private Object cursor;

    private static final ObjectMapper mapper = new ObjectMapper();

    public PresenceMessage() {}

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PresenceMessage", e);
        }
    }

    public static PresenceMessage fromJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, PresenceMessage.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Object getCursor() { return cursor; }
    public void setCursor(Object cursor) { this.cursor = cursor; }
    
}
