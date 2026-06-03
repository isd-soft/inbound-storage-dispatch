package com.isd.wms.controller;

import com.isd.wms.entity.User;
import com.isd.wms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// IMPORTĂ STATIC ACEASTĂ LINIE PENTRU CONFIGURAREA MOCK_MVC SECURITY:
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // 1. COMENTEAZĂ SAU ȘTERGE linia de deleteAll() ca să nu-ți mai șteargă userul manual!
        // userRepository.deleteAll();

        // 2. Verifică dacă userul de test există deja, ca să nu îl salvezi duplicat
        if (userRepository.findByUsername("test_user").isEmpty()) {
            User testUser = new User();
            testUser.setUsername("test_user");
            testUser.setEmail("test@isd.com");
            testUser.setPassword(passwordEncoder.encode("secret_password"));
            testUser.setUserRole("ROLE_DEV");
            userRepository.save(testUser);
        }
    }

    @Test
    void whenDataisCorrect_thenLoginReturnTokenJWT() throws Exception {
        String loginPayload = "{\"username\":\"test_user\",\"password\":\"secret_password\"}";

        // ADAUGĂ .with(csrf()) la sfârșitul cererii post ca MockMvc să nu blocheze testul cu 403
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void whenPasswordIsIncorrect_thenReturn401Unauthorized() throws Exception {
        String loginPayload = "{\"username\":\"test_user\",\"password\":\"incorrect_password\"}";

        // ADAUGĂ .with(csrf()) și aici
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload)
                        .with(csrf()))
                .andExpect(status().isUnauthorized()); // Schimbat din 4xxClientError în isUnauthorized() pentru precizie
    }
}