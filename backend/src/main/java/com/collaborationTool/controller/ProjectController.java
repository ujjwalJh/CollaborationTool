package com.collaborationTool.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.Project;
import com.collaborationTool.model.Team;
import com.collaborationTool.repository.ProjectRepository;
import com.collaborationTool.repository.TeamRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    // ✅ Create a project under a team
    @PostMapping("/team/{teamId}")
    public ResponseEntity<?> createProject(@PathVariable Long teamId, @RequestBody Project project) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Team not found");
        }

        project.setTeam(teamOpt.get());
        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(saved);
    }

    // ✅ Get all projects
    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // ✅ Get all projects for a team
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Project>> getProjectsByTeam(@PathVariable Long teamId) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        return teamOpt.map(team -> ResponseEntity.ok(team.getProjects().stream().toList()))
                      .orElse(ResponseEntity.notFound().build());
    }
}
