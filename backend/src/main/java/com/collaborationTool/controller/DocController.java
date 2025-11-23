package com.collaborationTool.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.Doc;
import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;
import com.collaborationTool.repository.DocRepository;
import com.collaborationTool.repository.UserRepository;
import com.collaborationTool.repository.WorkspaceRepository;
import com.collaborationTool.service.PermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocController {

    private final DocRepository docRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    // Create doc inside workspace - owner or member only
    @PostMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> createDoc(@PathVariable Long workspaceId,
                                       @RequestBody Doc payload,
                                       Authentication auth) {
        Optional<Workspace> wsOpt = workspaceRepository.findById(workspaceId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");

        Workspace ws = wsOpt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwnerOrMember(caller, ws)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Doc d = Doc.builder()
                .title(payload.getTitle() == null ? "Untitled" : payload.getTitle())
                .workspace(ws)
                .content(payload.getContent() == null ? "{\"type\":\"doc\",\"content\":[]}" : payload.getContent())
                .updatedAt(Instant.now())
                .build();

        docRepository.save(d);
        return ResponseEntity.ok(d);
    }

    // List docs (owner or member)
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> listDocs(@PathVariable Long workspaceId, Authentication auth) {
        Optional<Workspace> wsOpt = workspaceRepository.findById(workspaceId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");

        Workspace ws = wsOpt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwnerOrMember(caller, ws)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<Doc> docs = docRepository.findByWorkspaceId(workspaceId);
        return ResponseEntity.ok(docs);
    }

    // Get single doc - ensure workspace access
    @GetMapping("/{docId}")
    public ResponseEntity<?> getDoc(@PathVariable Long docId, Authentication auth) {
        Optional<Doc> docOpt = docRepository.findById(docId);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Doc doc = docOpt.get();
        Workspace ws = doc.getWorkspace();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwnerOrMember(caller, ws)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        return ResponseEntity.ok(doc);
    }

    // Save (update) doc - owner or member allowed to edit
    @PostMapping("/{docId}/save")
    public ResponseEntity<?> saveDoc(@PathVariable Long docId, @RequestBody Doc payload, Authentication auth) {
        Optional<Doc> docOpt = docRepository.findById(docId);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Doc doc = docOpt.get();
        Workspace ws = doc.getWorkspace();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwnerOrMember(caller, ws)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        doc.setContent(payload.getContent());
        doc.setUpdatedAt(Instant.now());
        docRepository.save(doc);
        return ResponseEntity.ok(doc);
    }
}
