package com.collaborationTool.ws;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an incoming edit. Supports:
 *  - full-content saves via 'content' (TipTap JSON)
 *  - incremental patches via 'from', 'to', 'text'
 *  - ProseMirror steps via 'steps' (preferred for TipTap v3)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditMessage {
    private String docId;
    private String user;

    /**
     * Optional TipTap full document JSON (stringified JSON).
     */
    private String content;

    // Optional incremental patch fields (positions)
    @JsonProperty("from")
    private Integer from;

    @JsonProperty("to")
    private Integer to;

    @JsonProperty("text")
    private String text;

    /**
     * Preferred: ProseMirror steps array serialized from transaction.steps.map(s => s.toJSON())
     * This should be a JSON array of step objects; keep it as List<Object> so Jackson keeps original structure.
     */
    @JsonProperty("steps")
    private List<Object> steps;

    @JsonProperty("clientId")
    private String clientId;

    private long timestamp;
}
