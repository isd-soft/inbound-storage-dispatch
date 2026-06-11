package com.isd.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.GlobalExceptionHandler;
import com.isd.wms.exception.OrderLineNotFoundException;
import com.isd.wms.service.OrderLineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig
@ContextConfiguration(classes = OrderLineControllerTest.TestConfig.class)
class OrderLineControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderLineService orderLineService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "/api/v1/order-lines";

    @BeforeEach
    void setUp() {
        reset(orderLineService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void getAllOrderLines_returns200AndList() throws Exception {
        OrderLineResponse response = new OrderLineResponse(1L, 1L, 1L, 1L, 10, Status.CREATED, null, null);
        when(orderLineService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderLineId").value(1L));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void getAllOrderLines_empty_returns200AndEmptyList() throws Exception {
        when(orderLineService.getAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void getOrderLineById_existingId_returns200() throws Exception {
        OrderLineResponse response = new OrderLineResponse(1L, 1L, 1L, 1L, 10, Status.CREATED, null, null);
        when(orderLineService.getOrderLineById(1L)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderLineId").value(1L));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void getOrderLineById_notFound_returns404() throws Exception {
        when(orderLineService.getOrderLineById(99L)).thenThrow(new OrderLineNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void deleteOrderLine_existingId_returns204() throws Exception {
        doNothing().when(orderLineService).deleteOrderLine(1L);

        mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void deleteOrderLine_notFound_returns404() throws Exception {
        doThrow(new OrderLineNotFoundException(99L)).when(orderLineService).deleteOrderLine(99L);

        mockMvc.perform(delete(BASE_URL + "/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public OrderLineService orderLineService() {
            return mock(OrderLineService.class);
        }

        @Bean
        public OrderLineController orderController(OrderLineService orderLineService) {
            return new OrderLineController(orderLineService);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
