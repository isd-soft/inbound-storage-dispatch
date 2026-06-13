package com.isd.wms.controller;

import com.isd.wms.dto.user.UserCreateRequest;
import com.isd.wms.dto.user.UserResponse;
import com.isd.wms.dto.user.UserUpdateRequest;
import com.isd.wms.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/supervisor/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser( @Valid @RequestBody UserCreateRequest request) {
        log.info("REST request to register User: {}, Role: {}", request.username(), request.userRole());
        try {
            userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User was successfully registered!",
                "username", request.username(),
                "role", request.userRole()
            ));
        } catch (AccessDeniedException e) {
            log.warn("Access denied during user registration for username: {}", request.username());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to register user {}: ", request.username(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,@Valid @RequestBody UserUpdateRequest request) {
        log.info("REST request to update User ID: {}", id);
        try {
            userService.updateUser(id, request);
            return ResponseEntity.ok(Map.of(
                "message", "User successfully updated!"
            ));
        } catch (AccessDeniedException e) {
            log.warn("Access denied during update for user ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update user ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        log.warn("REST request to DELETE User with ID: {}", id);
        try {
            userService.deleteUser(id);
            log.info("User with ID: {} was successfully deleted", id);
            return ResponseEntity.ok(Map.of(
                "message", "User successfully deleted!"
            ));
        } catch (AccessDeniedException e) {
            log.warn("Access denied during deletion attempt for user ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Deletion failed for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed for User request: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }


}
