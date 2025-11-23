package com.collaborationTool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collaborationTool.model.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwnerId(Long ownerId);
    List<Workspace> findByMembersId(Long userId);
}
