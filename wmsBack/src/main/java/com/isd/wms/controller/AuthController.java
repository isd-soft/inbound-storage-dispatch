package com.isd.wms.controller;

import com.example.isd.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> createAuthenticationToken(@RequestBody Map<String, String> loginRequest) throws Exception {

        String usernameOrEmail = loginRequest.get("username");
        String password = loginRequest.get("password");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(usernameOrEmail);
        final String jwt = jwtUtil.generateToken(userDetails.getUsername());

        return Map.of("token", jwt);
    }
}
