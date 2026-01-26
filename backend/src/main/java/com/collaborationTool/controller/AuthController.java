package com.collaborationTool.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collaborationTool.dto.JwtResponse;
import com.collaborationTool.dto.LoginRequest;
import com.collaborationTool.dto.RegisterRequest;
import com.collaborationTool.model.User;
import com.collaborationTool.repository.UserRepository;
import com.collaborationTool.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return Map.of("token", token);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest req) {
    // log.info("Login attempt for email: {}", req.email());
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> {
                    log.error("Invalid credentials for email: {}");
                    return new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"
                    );
                });
    
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            log.error("Invalid password");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid credentials"
            );
        }
    
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
