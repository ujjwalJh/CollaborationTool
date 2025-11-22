package com.collaborationTool.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collaborationTool.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> { }
