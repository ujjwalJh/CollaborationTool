package com.collaborationTool.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collaborationTool.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> { }
