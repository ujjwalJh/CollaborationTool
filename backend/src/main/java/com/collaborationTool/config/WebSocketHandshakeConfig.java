package com.collaborationTool.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Allows storing HTTP request attributes into the WebSocket session,
 * enabling JwtHandshakeInterceptor to read token even during SUBSCRIBE.
 */
@Component
public class WebSocketHandshakeConfig extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        // Store the request for later use by JwtHandshakeInterceptor
        attributes.put("HTTP_REQUEST", request);

        // Return null here — JwtHandshakeInterceptor sets the real Principal during CONNECT
        return null;
    }
}
