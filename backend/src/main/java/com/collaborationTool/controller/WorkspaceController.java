package com.collaborationTool.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;
import com.collaborationTool.repository.UserRepository;
import com.collaborationTool.repository.WorkspaceRepository;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceRepository workspaceRepository, UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createWorkspace(@RequestBody Workspace payload) {
        if (payload.getOwner() == null || payload.getOwner().getId() == null) {
            return ResponseEntity.badRequest().body("Owner required");
        }
        Optional<User> ownerOpt = userRepository.findById(payload.getOwner().getId());
        if (ownerOpt.isEmpty()) return ResponseEntity.badRequest().body("Owner not found");

        Workspace ws = Workspace.builder()
                .name(payload.getName())
                .owner(ownerOpt.get())
                .build();
        workspaceRepository.save(ws);
        return ResponseEntity.ok(ws);
    }

    @GetMapping
    public List<Workspace> listAll() {
        return workspaceRepository.findAll();
    }

    @GetMapping("/owner/{ownerId}")
    public List<Workspace> listByOwner(@PathVariable Long ownerId) {
        return workspaceRepository.findByOwnerId(ownerId);
    }

    @PostMapping("/{workspaceId}/addMember/{userId}")
    public ResponseEntity<?> addMember(@PathVariable Long workspaceId, @PathVariable Long userId) {
        var wsOpt = workspaceRepository.findById(workspaceId);
        var userOpt = userRepository.findById(userId);
        if (wsOpt.isEmpty() || userOpt.isEmpty()) return ResponseEntity.badRequest().body("not found");

        Workspace ws = wsOpt.get();
        ws.getMembers().add(userOpt.get());
        workspaceRepository.save(ws);
        return ResponseEntity.ok(ws);
    }
}
