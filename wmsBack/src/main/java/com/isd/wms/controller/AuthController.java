package com.isd.wms.controller;

import com.isd.wms.service.AuthService;
import com.isd.wms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String usernameOrEmail = loginRequest.get("username");
        String password = loginRequest.get("password");

        log.info("Authentication attempt for user/email: {}", usernameOrEmail);

        String token = authService.authenticateAndGenerateToken(usernameOrEmail, password);

        log.info("Authentication successful! Token is generated for user: {}", usernameOrEmail);

        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String password = payload.get("password");

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        try {
            boolean isVerified = userService.verifyEmail(token, password);
            if (isVerified) {
                log.info("Email verified successfully for token: {}", token);
                return ResponseEntity.ok(Map.of("message", "Account activated successfully! You can now log in."));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "The verification link has expired. The unverified account has been removed."
                ));
            }
        } catch (RuntimeException e) {
            log.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
