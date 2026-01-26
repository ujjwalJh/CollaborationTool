package com.collaborationTool.ws;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class PresenceMessage implements Serializable {

    private String type;
    private String docId;
    private String user;
    private Long userId;
    private String clientId;
    private Object cursor;

    public PresenceMessage() {}

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PresenceMessage: {}", this, e);
            throw new RuntimeException("Failed to serialize PresenceMessage", e);
        }
    }

    public static PresenceMessage fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, PresenceMessage.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PresenceMessage from json: {}", json, e);
            throw new RuntimeException(e);
        }
    }
}
