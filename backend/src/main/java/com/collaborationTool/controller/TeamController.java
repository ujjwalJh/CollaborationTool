package com.collaborationTool.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.Team;
import com.collaborationTool.model.User;
import com.collaborationTool.repository.TeamRepository;
import com.collaborationTool.repository.UserRepository;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;


    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        Team saved = teamRepository.save(team);
        return ResponseEntity.ok(saved);
    }


    @GetMapping
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @PostMapping("/{teamId}/addMember/{userId}")
    public ResponseEntity<?> addMember(@PathVariable Long teamId, @PathVariable Long userId) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (teamOpt.isEmpty() || userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Team or user not found");
        }

        Team team = teamOpt.get();
        User user = userOpt.get();

        team.getMembers().add(user);
        teamRepository.save(team);

        return ResponseEntity.ok(team);
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<Set<User>> getMembers(@PathVariable Long teamId) {
        return teamRepository.findById(teamId)
                .map(team -> ResponseEntity.ok(team.getMembers()))
                .orElse(ResponseEntity.notFound().build());
    }
}
