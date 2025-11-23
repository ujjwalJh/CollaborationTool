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

/**
 * Intercepts inbound STOMP frames.
 *
 * Responsibilities:
 *  - On CONNECT: validate JWT and attach a Principal (username/email).
 *  - On SUBSCRIBE: validate that the authenticated user is allowed to subscribe to the destination
 *    (e.g. /topic/doc.{docId} or /topic/presence.{docId}) by checking workspace membership/ownership.
 *
 * If the check fails we throw an exception which prevents the subscription/connection.
 */
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

        // ---------- CONNECT: validate JWT and set Principal ----------
        if (StompCommand.CONNECT.equals(command)) {
            // Try typical Authorization header first
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            }

            // fallback: some clients send token header named "token"
            if (token == null || token.isBlank()) {
                String t = accessor.getFirstNativeHeader("token");
                if (t != null && !t.isBlank()) token = t.trim();
            }

            if (token == null || token.isBlank()) {
                log.warn("WebSocket CONNECT missing token header");
                throw new IllegalArgumentException("Missing JWT token for WebSocket CONNECT");
            }

            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket CONNECT invalid token");
                throw new IllegalArgumentException("Invalid JWT token for WebSocket CONNECT");
            }

            String email = jwtUtil.extractUsername(token);
            if (email == null || email.isBlank()) {
                log.warn("WebSocket CONNECT token has no subject (username/email)");
                throw new IllegalArgumentException("Invalid JWT token (no subject)");
            }

            // set a minimal Principal so downstream can get principal name()
            Principal userPrincipal = new Principal() {
                @Override
                public String getName() {
                    return email;
                }
            };

            accessor.setUser(userPrincipal);
            log.debug("WebSocket CONNECT accepted for user={}", email);
            return message;
        }

        // ---------- SUBSCRIBE: validate destination access ----------
        if (StompCommand.SUBSCRIBE.equals(command)) {
            Principal principal = accessor.getUser();
            if (principal == null || principal.getName() == null) {
                log.warn("SUBSCRIBE without authenticated principal");
                throw new IllegalArgumentException("Not authenticated");
            }

            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                // Allow subscriptions that don't target protected destinations
                return message;
            }

            // We only enforce for doc / presence topics used by editor:
            // e.g. "/topic/doc.123" or "/topic/presence.123"
            Long docId = extractDocIdFromDestination(destination);
            if (docId == null) {
                // Not a doc/presence topic we care about, allow
                return message;
            }

            String email = principal.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Authenticated principal not found in DB: {}", email);
                throw new IllegalArgumentException("User not found");
            }

            User user = userOpt.get();

            Optional<Doc> docOpt = docRepository.findById(docId);
            if (docOpt.isEmpty()) {
                log.warn("SUBSCRIBE to non-existing doc: {}", docId);
                throw new IllegalArgumentException("Document not found");
            }

            Doc doc = docOpt.get();
            Workspace ws = doc.getWorkspace();
            if (ws == null) {
                log.warn("Document {} missing workspace relation", docId);
                throw new IllegalArgumentException("Document workspace missing");
            }

            // Final permission check: owner or member allowed
            if (!permissionService.isOwnerOrMember(user, ws)) {
                log.info("User {} is not member/owner of workspace {} -> deny subscribe to doc {}", user.getEmail(), ws.getId(), docId);
                throw new IllegalArgumentException("Forbidden: not a member of workspace");
            }

            // allowed
            log.debug("User {} allowed to subscribe to doc {} (workspace {})", user.getEmail(), docId, ws.getId());
            return message;
        }

        // For other frames, pass through
        return message;
    }

    /**
     * Helper: extract numeric doc id from destinations like:
     *  - /topic/doc.123
     *  - /topic/presence.123
     *
     * Returns null if the destination is not a recognized doc/presence topic.
     */
    private Long extractDocIdFromDestination(String destination) {
        if (destination == null) return null;

        // common prefixes we protect
        List<String> prefixes = List.of("/topic/doc.", "/topic/presence.", "/topic/presence/");
        for (String prefix : prefixes) {
            if (destination.startsWith(prefix)) {
                String tail = destination.substring(prefix.length());
                // tail may contain extra path segments or query — attempt to parse leading number
                StringBuilder sb = new StringBuilder();
                for (char c : tail.toCharArray()) {
                    if (Character.isDigit(c)) sb.append(c);
                    else break;
                }
                if (sb.length() == 0) return null;
                try {
                    return Long.valueOf(sb.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        // also support pattern "/topic/doc/" + id
        if (destination.startsWith("/topic/doc/") || destination.startsWith("/topic/presence/")) {
            String[] parts = destination.split("/");
            String last = parts[parts.length - 1];
            try {
                return Long.valueOf(last);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
