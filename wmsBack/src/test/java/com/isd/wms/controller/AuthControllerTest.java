package com.isd.wms.controller;

import com.isd.wms.security.SecurityConfig;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig
@ContextConfiguration(classes = AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        reset(userRepository);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        User testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test@isd.com");
        testUser.setPassword(passwordEncoder.encode("secret_password"));
        testUser.setUserRole("ROLE_DEV");

        when(userRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("incorrect_user")).thenReturn(Optional.empty());
    }

    @Test
    void whenDataisCorrect_thenLoginReturnTokenJWT() throws Exception {
        String loginPayload = "{\"username\":\"test_user\",\"password\":\"secret_password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void whenPasswordIsIncorrect_thenReturn401Unauthorized() throws Exception {
        String loginPayload = "{\"username\":\"test_user\",\"password\":\"incorrect_password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class})
    static class TestConfig {

        @Bean
        public AuthController authController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
            return new AuthController(userRepository, passwordEncoder);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}