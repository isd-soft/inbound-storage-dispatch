package com.isd.wms.service;

import com.isd.wms.exception.InvalidCredentialsException;
import com.isd.wms.exception.UserNotVerifiedException;
import com.isd.wms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public String authenticateAndGenerateToken(String username, String password) {
        log.info("Attempting authentication for user: '{}'", username);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            log.debug("Authentication successful via AuthenticationManager for user: '{}'", username);

        } catch (DisabledException e) {
            log.warn("Authentication failed: User '{}' is disabled/not verified.", username);
            throw new UserNotVerifiedException("Please verify your email before logging in.");
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed: Invalid credentials provided for user '{}'", username);
            throw new InvalidCredentialsException("Incorrect username or password.");
        } catch (Exception e) {
            log.error("Unexpected error occurred during authentication for user '{}': ", username, e);
            throw e;
        }

        log.debug("Loading user details to generate JWT for user: '{}'", username);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        String token = jwtUtil.generateToken(userDetails.getUsername(), role);

        log.info("User '{}' successfully authenticated. Token issued with role: [{}]", username, role);
        return token;
    }
}
