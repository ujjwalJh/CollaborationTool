package com.collaborationTool.service;

import org.springframework.stereotype.Service;

import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;

@Service
public class PermissionService {
    public boolean isOwner(User user, Workspace workspace) {
        if (user == null || workspace == null || workspace.getOwner() == null) return false;
        return user.getId() != null && user.getId().equals(workspace.getOwner().getId());
    }

    public boolean isMember(User user, Workspace workspace) {
        if (user == null || workspace == null) return false;
        return workspace.getMembers().stream()
                .anyMatch(u -> u.getId() != null && u.getId().equals(user.getId()));
    }

    public boolean isOwnerOrMember(User user, Workspace workspace) {
        return isOwner(user, workspace) || isMember(user, workspace);
    }
}
