package com.collaborationTool.dto;

public record LoginRequest(
        String email,
        String password
) {}
