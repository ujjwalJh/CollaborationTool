package com.collaborationTool.ws;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditMessage {
    private String docId;
    private String user;
    private String content;

    @JsonProperty("from")
    private Integer from;

    @JsonProperty("to")
    private Integer to;

    @JsonProperty("text")
    private String text;
    
    @JsonProperty("steps")
    private List<Object> steps;

    @JsonProperty("clientId")
    private String clientId;

    private long timestamp;
}
