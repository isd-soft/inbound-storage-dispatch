package com.isd.wms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Dezactivăm CSRF deoarece folosim JWT (stateless)
                .csrf(csrf -> csrf.disable())

                // Reguli de autorizare a rutelor API
                .authorizeHttpRequests(auth -> auth
                        // Permitem tuturor accesul la login/autentificare
                        .requestMatchers("/api/auth/**").permitAll()

                        // Documentația Swagger/OpenAPI permisă public
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Restricții pe bază de roluri
                        .requestMatchers("/api/supervisor/**").hasAnyRole("SUPERVISOR", "DEV")
                        .requestMatchers("/api/operator/**").hasAnyRole("OPERATOR", "DEV")

                        // Orice altă cerere trebuie să fie autentificată
                        .anyRequest().authenticated()
                )

                // Setăm sesiunea ca fiind Stateless (fără stare, specific JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    // Criptăm parolele folosind algoritmul BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}