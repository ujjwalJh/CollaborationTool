package com.collaborationTool.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;
import com.collaborationTool.repository.UserRepository;
import com.collaborationTool.repository.WorkspaceRepository;
import com.collaborationTool.service.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public static class CreateWorkspaceRequest {
        public String name;
    }

    public static class RenameWorkspaceRequest {
        public String name;
    }

    
    @PostMapping
    public ResponseEntity<?> createWorkspace(@RequestBody CreateWorkspaceRequest payload, Authentication auth) {
        if (payload == null || payload.name == null || payload.name.isBlank()) {
            return ResponseEntity.badRequest().body("Workspace name is required");
        }

        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Workspace ws = Workspace.builder()
                .name(payload.name.trim())
                .owner(owner)
                .build();

        
        ws.getMembers().add(owner);

        workspaceRepository.save(ws);

        return ResponseEntity.created(URI.create("/api/workspaces/" + ws.getId())).body(ws);
    }

    @GetMapping
    public List<Workspace> listAll() {
        return workspaceRepository.findAll();
    }

    
    @GetMapping("/owner/{ownerId}")
    public List<Workspace> listByOwner(@PathVariable Long ownerId) {
        return workspaceRepository.findByOwnerId(ownerId);
    }


    @GetMapping("/member/{userId}")
    public List<Workspace> listByMember(@PathVariable Long userId) {
        return workspaceRepository.findByMembersId(userId);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkspace(@PathVariable Long id, Authentication auth) {
        Optional<Workspace> opt = workspaceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Workspace ws = opt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwnerOrMember(caller, ws)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        return ResponseEntity.ok(ws);
    }


    @PutMapping("/{id}/rename")
    public ResponseEntity<?> renameWorkspace(@PathVariable Long id,
                                             @RequestBody RenameWorkspaceRequest payload,
                                             Authentication auth) {
        if (payload == null || payload.name == null || payload.name.isBlank()) {
            return ResponseEntity.badRequest().body("Name is required");
        }

        Optional<Workspace> opt = workspaceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Workspace ws = opt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwner(caller, ws)) {
            return ResponseEntity.status(403).body("Only owner can rename workspace");
        }

        ws.setName(payload.name.trim());
        workspaceRepository.save(ws);
        return ResponseEntity.ok(ws);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkspace(@PathVariable Long id, Authentication auth) {
        Optional<Workspace> opt = workspaceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Workspace ws = opt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwner(caller, ws)) {
            return ResponseEntity.status(403).body("Only owner can delete workspace");
        }

        workspaceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{workspaceId}/addMember/{userId}")
    public ResponseEntity<?> addMember(@PathVariable Long workspaceId,
                                       @PathVariable Long userId,
                                       Authentication auth) {
        var wsOpt = workspaceRepository.findById(workspaceId);
        var userOpt = userRepository.findById(userId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        Workspace ws = wsOpt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwner(caller, ws)) {
            log.error("Only owner can add members");
            return ResponseEntity.status(403).body("Only owner can add members");
        }

        User toAdd = userOpt.get();
        ws.getMembers().add(toAdd);
        workspaceRepository.save(ws);
        return ResponseEntity.ok(ws);
    }


    @PostMapping("/{workspaceId}/removeMember/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long workspaceId,
                                          @PathVariable Long userId,
                                          Authentication auth) {
        var wsOpt = workspaceRepository.findById(workspaceId);
        var userOpt = userRepository.findById(userId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        Workspace ws = wsOpt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (!permissionService.isOwner(caller, ws)) {
            return ResponseEntity.status(403).body("Only owner can remove members");
        }

        if (ws.getOwner() != null && ws.getOwner().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("Cannot remove owner from workspace");
        }

        ws.getMembers().removeIf(u -> u.getId().equals(userId));
        workspaceRepository.save(ws);
        return ResponseEntity.ok(ws);
    }


    @PostMapping("/{workspaceId}/leave")
    public ResponseEntity<?> leaveWorkspace(@PathVariable Long workspaceId, Authentication auth) {
        var wsOpt = workspaceRepository.findById(workspaceId);
        if (wsOpt.isEmpty()) return ResponseEntity.badRequest().body("Workspace not found");

        Workspace ws = wsOpt.get();
        String email = auth.getName();
        User caller = userRepository.findByEmail(email).orElse(null);

        if (caller == null) return ResponseEntity.status(403).body("Forbidden");

        if (permissionService.isOwner(caller, ws)) {
            return ResponseEntity.badRequest().body("Owner cannot leave workspace. Assign a new owner before leaving.");
        }

        boolean removed = ws.getMembers().removeIf(u -> u.getId() != null && u.getId().equals(caller.getId()));
        if (removed) {
            workspaceRepository.save(ws);
            return ResponseEntity.ok(ws);
        } else {
            return ResponseEntity.badRequest().body("You are not a member of this workspace");
        }
    }
}