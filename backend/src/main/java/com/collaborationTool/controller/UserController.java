package com.collaborationTool.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaborationTool.model.User;
import com.collaborationTool.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    // 👥 Get all users (ADMIN only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 👤 Get user by email (any authenticated user)
    @GetMapping("/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 🔄 Promote a user to ADMIN (ADMIN only)
    @PutMapping("/{email}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public User promoteToAdmin(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(com.collaborationTool.model.Role.ADMIN);
        return userRepository.save(user);
    }
}