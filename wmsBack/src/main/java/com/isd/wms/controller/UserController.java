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
@RequestMapping("/api/supervisor/users") // Rută protejată automat în SecurityConfig!
@Slf4j // Adaugă suportul nativ pentru logare prin SLF4J (Lombok)
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
        String role = registerRequest.get("userRole"); // ex: ROLE_OPERATOR, ROLE_SUPERVISOR

        log.info("Tentativă de înregistrare utilizator nou. Username: {}, Email: {}, Rol solicitat: {}", username, email, role);

        // 1. Validări de bază
        if (username == null || email == null || password == null || role == null) {
            log.warn("Înregistrare eșuată: Câmpuri lipsă în cerere.");
            return ResponseEntity.badRequest().body(Map.of("error", "Toate câmpurile (username, email, password, userRole) sunt obligatorii."));
        }

        // 2. Verifică dacă username-ul există deja
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Înregistrare eșuată: Username-ul '{}' este deja utilizat.", username);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Acest username este deja luat."));
        }

        // 3. Verifică dacă email-ul există deja
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Înregistrare eșuată: Email-ul '{}' este deja utilizat.", email);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Acest email este deja înregistrat."));
        }

        // 4. Crearea și salvarea noului utilizator
        try {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            // Criptăm parola obligatoriu prin BCrypt înainte de salvare în DB
            newUser.setPassword(passwordEncoder.encode(password));

            // Pentru siguranță, dacă rolul trimis nu are prefixul ROLE_, îl adăugăm automat
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role.toUpperCase();
            }
            newUser.setUserRole(role);

            userRepository.save(newUser);
            log.info("Utilizatorul '{}' a fost înregistrat cu succes cu rolul '{}'.", username, role);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Utilizator înregistrat cu succes!",
                    "username", username,
                    "role", role
            ));
        } catch (Exception e) {
            log.error("Eroare neșteptată în timpul înregistrării utilizatorului '{}': ", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Eroare internă de server."));
        }
    }
}
