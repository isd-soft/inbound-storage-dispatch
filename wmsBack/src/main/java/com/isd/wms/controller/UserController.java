package com.isd.wms.controller;

import com.isd.wms.entity.User;
import com.isd.wms.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/supervisor/users")
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String email = registerRequest.get("email");
        String password = registerRequest.get("password");
        String role = registerRequest.get("userRole");

        log.info("New user registration attempt. Username: {}, Email: {}, Requested role: {}", username, email, role);

        if (username == null || email == null || password == null || role == null) {
            log.warn("Registration failed: Missing fields in request.");
            return ResponseEntity.badRequest().body(Map.of("error", "All fields (username, email, password, userRole) are required."));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registration failed: Username '{}' is already in use.", username);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "This username is already taken."));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Registration failed: E-mail '{}' is already in use.", email);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "This username is already registered."));
        }

        try {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);

            newUser.setPassword(passwordEncoder.encode(password));

            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role.toUpperCase();
            }
            newUser.setUserRole(role);

            userRepository.save(newUser);
            log.info("User '{}' was successfully registered with the role '{}'.", username, role);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User was successfully registered!",
                    "username", username,
                    "role", role
            ));
        } catch (Exception e) {
            log.error("Unexpected error during user registration '{}': ", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error."));
        }
    }
}
