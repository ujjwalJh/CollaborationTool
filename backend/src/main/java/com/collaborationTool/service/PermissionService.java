package com.collaborationTool.service;

import org.springframework.stereotype.Service;

import com.collaborationTool.model.User;
import com.collaborationTool.model.Workspace;

@Service
public class PermissionService {

    /**
     * Returns true if user is the owner of the workspace.
     */
    public boolean isOwner(User user, Workspace workspace) {
        if (user == null || workspace == null || workspace.getOwner() == null) return false;
        return user.getId() != null && user.getId().equals(workspace.getOwner().getId());
    }

    /**
     * Returns true if user is a member of the workspace (includes owner if owner is in members set).
     */
    public boolean isMember(User user, Workspace workspace) {
        if (user == null || workspace == null) return false;
        return workspace.getMembers().stream()
                .anyMatch(u -> u.getId() != null && u.getId().equals(user.getId()));
    }

    /**
     * Convenience: owner OR member allowed.
     */
    public boolean isOwnerOrMember(User user, Workspace workspace) {
        return isOwner(user, workspace) || isMember(user, workspace);
    }
}
