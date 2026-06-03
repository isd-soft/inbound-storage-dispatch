package com.isd.wms.controller;

import com.isd.wms.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j // Adaugă suportul de SLF4J (log.info, log.error etc.)
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
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> loginRequest) {
        String usernameOrEmail = loginRequest.get("username");
        String password = loginRequest.get("password");

        log.info("Tentativă de autentificare pentru utilizatorul/email-ul: {}", usernameOrEmail);

        try {
            // Autentificarea nativă Spring Security (verifică automat hash-ul parolei în DB)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
            );
        } catch (BadCredentialsException e) {
            log.warn("Autentificare eșuată pentru '{}': Credențiale incorecte.", usernameOrEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Username sau parolă incorectă."));
        } catch (Exception e) {
            log.error("Eroare neșteptată la autentificarea utilizatorului '{}': ", usernameOrEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la procesarea autentificării."));
        }

        // Dacă codul ajunge aici, înseamnă că autentificarea a reușit!
        final UserDetails userDetails = userDetailsService.loadUserByUsername(usernameOrEmail);
        final String jwt = jwtUtil.generateToken(userDetails.getUsername());

        log.info("Autentificare reușită! Token generat pentru utilizatorul: {}", userDetails.getUsername());

        return ResponseEntity.ok(Map.of("token", jwt));
    }
}