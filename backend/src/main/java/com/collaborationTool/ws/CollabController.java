package com.collaborationTool.ws;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.collaborationTool.repository.DocRepository;

@Controller
public class CollabController {

    private final Logger log = LoggerFactory.getLogger(CollabController.class);

    private final SimpMessagingTemplate messaging;
    private final DocRepository docRepository;

    public CollabController(SimpMessagingTemplate messaging, DocRepository docRepository) {
        this.messaging = messaging;
        this.docRepository = docRepository;
    }

    /**
     * MAIN REAL-TIME EDIT HANDLER
     * ---------------------------
     * Handles:
     *   ✔ ProseMirror steps (if ever used)
     *   ✔ Delta patches (fallback)
     *   ✔ Full JSON content (TipTap JSON)
     *
     * Your frontend uses FULL CONTENT syncing.
     */
    @MessageMapping("/edit")
    public void onEdit(EditMessage edit) {
        try {
            if (edit == null) {
                log.warn("Received null EditMessage");
                return;
            }

            final String docId = edit.getDocId();
            if (docId == null || docId.isBlank()) {
                log.warn("Received EditMessage with missing docId: {}", edit);
                return;
            }

            final String topic = "/topic/doc." + docId;

            log.info("onEdit → docId={}  user={}  clientId={}",
                    docId, edit.getUser(), edit.getClientId());

            // ========== 1) BROADCAST FULL CONTENT (your frontend uses this) ==========
            String content = edit.getContent();
            if (content != null) {

                Map<String, Object> msg = new HashMap<>();
                msg.put("docId", docId);
                msg.put("user", edit.getUser());
                msg.put("clientId", edit.getClientId());
                msg.put("content", content); // IMPORTANT
                msg.put("timestamp", edit.getTimestamp());

                messaging.convertAndSend(topic, msg);

                log.debug("Broadcasted full-content update → {}", topic);

                // Persist to DB
                try {
                    Long id = Long.valueOf(docId);
                    docRepository.findById(id).ifPresent(doc -> {
                        doc.setContent(content);
                        doc.setUpdatedAt(Instant.now());
                        docRepository.save(doc);
                        log.debug("Saved doc {} to DB (updatedAt={})", id, doc.getUpdatedAt());
                    });
                } catch (Exception e) {
                    log.error("Failed to persist doc {} -> {}", docId, e.getMessage());
                }

                return; // <-- THIS prevents falling into step/delta blocks
            }

            // ========== 2) PROSEMIRROR STEPS (not used right now but supported) ==========
            List<Object> steps = edit.getSteps();
            if (steps != null && !steps.isEmpty()) {

                Map<String, Object> msg = new HashMap<>();
                msg.put("docId", docId);
                msg.put("user", edit.getUser());
                msg.put("clientId", edit.getClientId());
                msg.put("steps", steps);
                msg.put("timestamp", edit.getTimestamp());

                messaging.convertAndSend(topic, msg);

                log.debug("Broadcasted steps → {} ({} steps)", topic, steps.size());
                return;
            }

            // ========== 3) DELTA PATCH FALLBACK ==========
            if (edit.getFrom() != null && edit.getTo() != null && edit.getText() != null) {

                Map<String, Object> delta = new HashMap<>();
                delta.put("docId", docId);
                delta.put("user", edit.getUser());
                delta.put("clientId", edit.getClientId());
                delta.put("from", edit.getFrom());
                delta.put("to", edit.getTo());
                delta.put("text", edit.getText());
                delta.put("timestamp", edit.getTimestamp());

                messaging.convertAndSend(topic, delta);

                log.debug("Broadcasted delta → {} (from={},to={},textLength={})",
                        topic, edit.getFrom(), edit.getTo(), edit.getText().length());
            }

        } catch (Exception e) {
            log.error("Unhandled error in /edit handler: {}", e.getMessage(), e);
        }
    }

    /**
     * PRESENCE / CURSOR HANDLER
     */
    // @MessageMapping("/presence")
    // public void onPresence(PresenceMessage p) {
    //     if (p == null || p.getDocId() == null) {
    //         log.warn("Invalid presence message: {}", p);
    //         return;
    //     }

    //     final String topic = "/topic/presence." + p.getDocId();
    //     messaging.convertAndSend(topic, p);
    // }
}
