// src/main/java/com/collaborationTool/config/JwtHandshakeInterceptor.java
package com.collaborationTool.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.collaborationTool.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP CONNECT headers -> Authorization
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token == null || !jwtUtil.validateToken(token)) {
                // reject handshake
                throw new IllegalArgumentException("Invalid or missing JWT during WebSocket handshake");
            }

            String email = jwtUtil.extractUsername(token);
            accessor.setUser(() -> email); // set principal (so messagingTemplate can resolve user)
        }

        return message;
    }
}
