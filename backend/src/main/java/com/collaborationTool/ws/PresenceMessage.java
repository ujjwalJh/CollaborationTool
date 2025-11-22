package com.collaborationTool.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message for presence / cursor / join/leave events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceMessage {
    private String docId;
    private String user;
    private String action;   // "join", "leave", "cursor", etc.
    private int cursorPos;   // optional cursor position
    private long timestamp;
}
