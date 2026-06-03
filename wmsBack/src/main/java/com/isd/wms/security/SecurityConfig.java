package com.isd.wms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    // Injectăm filtrul JWT prin constructor
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Dezactivăm CSRF deoarece folosim JWT (Stateless)
                .csrf(csrf -> csrf.disable())

                // 2. Dezactivăm formLogin și httpBasic implicite din Spring Security
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 3. Configurăm regulile de autorizare a rutelor
                .authorizeHttpRequests(auth -> auth
                        // Rutele publice: autentificare și documentație API (Swagger)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // MODIFICAT: Înlocuit .hasAnyRole cu .hasAnyAuthority pentru a verifica stringul exact din baza de date
                        .requestMatchers("/api/supervisor/**").hasAnyAuthority("ROLE_SUPERVISOR", "ROLE_DEV", "SUPERVISOR", "DEV")
                        .requestMatchers("/api/operator/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_DEV", "OPERATOR", "DEV")

                        // Orice altă cerere necesită un utilizator autentificat
                        .anyRequest().authenticated()
                )

                // 4. Setăm managementul sesiunilor ca fiind complet STATELESS
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // 5. Adăugăm filtrul nostru JWT înaintea filtrului standard de Username & Password
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean esențial solicitat de AuthController pentru procesul de login nativ
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Strategia de criptare a parolelor utilizatorilor din baza de date
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}