package com.isd.wms.controller;

import com.isd.wms.entity.User;
import com.isd.wms.service.AuthService;
import com.isd.wms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Value("${wms.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String usernameOrEmail = loginRequest.get("username");
        String password = loginRequest.get("password");

        log.info("Authentication attempt for user/email: {}", usernameOrEmail);

        String token = authService.authenticateAndGenerateToken(usernameOrEmail, password);

        log.info("Authentication successful! Token is generated for user: {}", usernameOrEmail);

        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        ModelAndView mav = new ModelAndView();
        try {
            User user = userService.verifyEmail(token);
            log.info("Email verified successfully for token: {}", token);

            mav.setViewName("email/verify-success");

            mav.addObject("username", user.getUsername());
            mav.addObject("loginUrl", frontendUrl + "/login");

        } catch (RuntimeException e) {
            log.warn("Email verification failed: {}", e.getMessage());

            mav.setViewName("email/verify-error");
            mav.addObject("error", e.getMessage());
        }

        return mav;
    }
}
