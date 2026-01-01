package com.collaborationTool.config;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.collaborationTool.model.Doc;
import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;
import com.collaborationTool.repository.DocRepository;
import com.collaborationTool.repository.UserRepository;
import com.collaborationTool.security.JwtUtil;
import com.collaborationTool.service.PermissionService;

import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final DocRepository docRepository;
    private final PermissionService permissionService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            }

            if (token == null || token.isBlank()) {
                token = accessor.getFirstNativeHeader("token");
            }

            if (token == null || token.isBlank()) {
                log.warn("WS CONNECT missing token");
                throw new IllegalArgumentException("Missing token");
            }

            if (!jwtUtil.validateToken(token)) {
                log.warn("WS CONNECT invalid token");
                throw new IllegalArgumentException("Invalid token");
            }

            String email = jwtUtil.extractUsername(token);

            Principal principal = () -> email;

            // STORE PRINCIPAL FOR FUTURE FRAMES (SockJS fix)
            accessor.setUser(principal);
            accessor.getSessionAttributes().put("USER_PRINCIPAL", principal);

            log.info("WS CONNECT authenticated: {}", email);
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {

            Principal principal = accessor.getUser();
            if (principal == null) {
                principal = (Principal) accessor.getSessionAttributes().get("USER_PRINCIPAL");
            }

            if (principal == null) {
                log.warn("SUBSCRIBE missing principal");
                throw new IllegalArgumentException("Not authenticated");
            }

            String destination = accessor.getDestination();
            if (destination == null) {
                return message; // ignore unknown topics
            }

            Long docId = extractDocIdFromDestination(destination);
            if (docId == null) {
                return message; // not a protected topic
            }

            String email = principal.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("SUBSCRIBE user not found: {}", email);
                throw new IllegalArgumentException("User not found");
            }

            User user = userOpt.get();

            Optional<Doc> docOpt = docRepository.findById(docId);
            if (docOpt.isEmpty()) {
                log.warn("SUBSCRIBE to non-existing doc: {}", docId);
                throw new IllegalArgumentException("Doc not found");
            }

            Doc doc = docOpt.get();
            Workspace ws = doc.getWorkspace();

            if (!permissionService.isOwnerOrMember(user, ws)) {
                log.info("Forbidden SUBSCRIBE: {} not member of workspace {}", email, ws.getId());
                throw new IllegalArgumentException("Forbidden");
            }

            log.debug("SUBSCRIBE allowed: {} -> doc {}", email, docId);
            return message;
        }

        return message;
    }

    
    private Long extractDocIdFromDestination(String destination) {

        List<String> prefixes = List.of("/topic/doc.", "/topic/presence.", "/topic/presence/");

        for (String prefix : prefixes) {
            if (destination.startsWith(prefix)) {
                String tail = destination.substring(prefix.length());
                StringBuilder sb = new StringBuilder();
                for (char c : tail.toCharArray()) {
                    if (Character.isDigit(c)) sb.append(c); else break;
                }
                if (sb.length() > 0) {
                    return Long.valueOf(sb.toString());
                }
            }
        }

        if (destination.startsWith("/topic/doc/")) {
            try { return Long.valueOf(destination.substring(11)); }
            catch (Exception e) { return null; }
        }

        return null;
    }
}
