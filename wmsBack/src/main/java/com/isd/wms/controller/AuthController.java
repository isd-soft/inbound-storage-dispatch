package com.isd.wms.controller;

import com.isd.wms.service.UserService;
import com.isd.wms.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${wms.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> loginRequest) {
        String usernameOrEmail = loginRequest.get("username");
        String password = loginRequest.get("password");

        log.info("Authentification attempt for user/email: {}", usernameOrEmail);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
            );
        } catch (DisabledException e) {
            log.warn("Authentification failed for '{}': Email not verified.", usernameOrEmail);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Please verify your email before logging in."));
        } catch (BadCredentialsException e) {
            log.warn("Authentification failed for '{}': Incorrect credentials.", usernameOrEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Incorrect username or password."));
        } catch (Exception e) {
            log.error("Unexpected error during user authentification '{}': ", usernameOrEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing authentification."));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(usernameOrEmail);
        final String jwt = jwtUtil.generateToken(
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );

        log.info("Authentification successful! Token is generated for user: {}", userDetails.getUsername());

        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        try {
            String username = userService.verifyEmail(token);
            log.info("Email verified successfully for user: {}", username);
            ModelAndView mav = new ModelAndView("email/verify-success");
            mav.addObject("username", username);
            mav.addObject("loginUrl", frontendUrl + "/login");
            return mav;
        } catch (RuntimeException e) {
            log.warn("Email verification failed: {}", e.getMessage());
            ModelAndView mav = new ModelAndView("email/verify-error");
            mav.addObject("error", e.getMessage());
            return mav;
        }
    }
}