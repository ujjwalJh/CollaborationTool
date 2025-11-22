// package com.collaborationTool.config;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.Message;
// import org.springframework.messaging.MessageChannel;
// import org.springframework.messaging.simp.stomp.StompCommand;
// import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
// import org.springframework.messaging.support.ChannelInterceptor;

// import com.collaborationTool.security.JwtUtil;

// import lombok.RequiredArgsConstructor;

// @Configuration
// @RequiredArgsConstructor
// public class WebSocketAuthConfig implements ChannelInterceptor {

//     private final JwtUtil jwtUtil;

//     @Override
//     public Message<?> preSend(Message<?> message, MessageChannel channel) {

//         StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

//         // Only handle CONNECT frames here
//         if (StompCommand.CONNECT.equals(accessor.getCommand())) {

//             String token = accessor.getFirstNativeHeader("Authorization");
//             if (token != null && token.startsWith("Bearer ")) {
//                 token = token.substring(7);
//             }

//             if (token == null || !jwtUtil.validateToken(token)) {
//                 // Throwing an exception will reject the handshake
//                 throw new IllegalArgumentException("Invalid or missing JWT during WebSocket handshake");
//             }

//             String email = jwtUtil.extractUsername(token);
//             // set a Principal so that messagingTemplate / SimpMessageHeaderAccessor can see the user
//             accessor.setUser(() -> email);
//         }

//         return message;
//     }
// }
