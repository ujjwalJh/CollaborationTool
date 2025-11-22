package com.collaborationTool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collaborationTool.model.Doc;

public interface DocRepository extends JpaRepository<Doc, Long> {
    List<Doc> findByWorkspaceId(Long workspaceId);
}
