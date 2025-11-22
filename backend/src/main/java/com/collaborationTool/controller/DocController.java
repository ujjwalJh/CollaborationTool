package com.collaborationTool.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.collaborationTool.model.Doc;
import com.collaborationTool.model.Workspace;
import com.collaborationTool.repository.DocRepository;
import com.collaborationTool.repository.WorkspaceRepository;

@RestController
@RequestMapping("/api/docs")
public class DocController {

    private final DocRepository docRepository;
    private final WorkspaceRepository workspaceRepository;

    public DocController(DocRepository docRepository, WorkspaceRepository workspaceRepository) {
        this.docRepository = docRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @PostMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> createDoc(@PathVariable Long workspaceId, @RequestBody Doc payload) {
        Optional<Workspace> wsOpt = workspaceRepository.findById(workspaceId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");

        Doc d = Doc.builder()
                .title(payload.getTitle() == null ? "Untitled" : payload.getTitle())
                .workspace(wsOpt.get())
                .content(payload.getContent() == null ? "{\"type\":\"doc\",\"content\":[]}" : payload.getContent())
                .updatedAt(Instant.now())
                .build();

        docRepository.save(d);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/workspace/{workspaceId}")
    public List<Doc> listDocs(@PathVariable Long workspaceId) {
        return docRepository.findByWorkspaceId(workspaceId);
    }

    @GetMapping("/{docId}")
    public ResponseEntity<?> getDoc(@PathVariable Long docId) {
        return docRepository.findById(docId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{docId}/save")
    public ResponseEntity<?> saveDoc(@PathVariable Long docId, @RequestBody Doc payload) {
        Optional<Doc> docOpt = docRepository.findById(docId);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();
        Doc doc = docOpt.get();
        doc.setContent(payload.getContent());
        doc.setUpdatedAt(Instant.now());
        docRepository.save(doc);
        return ResponseEntity.ok(doc);
    }
}
