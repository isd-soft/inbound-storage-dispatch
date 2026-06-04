package com.isd.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isd.wms.dto.replenishment.ReplenishmentTaskCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskResponse;
import com.isd.wms.dto.replenishment.ReplenishmentTaskSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskUpdateRequest;
import com.isd.wms.enums.ReplenishmentTaskStatus;
import com.isd.wms.exception.ReplenishmentTaskNotFoundException;
import com.isd.wms.service.ReplenishmentTaskService;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig
@ContextConfiguration(classes = ReplenishmentTaskControllerTest.TestConfig.class)
class ReplenishmentTaskControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ReplenishmentTaskService replenishmentTaskService;

    @BeforeEach
    void setUp() {
        reset(replenishmentTaskService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void createReplenishmentTask_withSupervisorRole_returnsCreated() throws Exception {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 2L, 3L);
        ReplenishmentTaskResponse response = new ReplenishmentTaskResponse(1L, 1L, null, 10, ReplenishmentTaskStatus.CREATED, 2L, 3L, null);

        when(replenishmentTaskService.createReplenishmentTask(any(ReplenishmentTaskCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/replenishment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createReplenishmentTask_withOperatorRole_returnsForbidden() throws Exception {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 2L, 3L);

        mockMvc.perform(post("/api/replenishment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReplenishmentTask_unauthenticated_returnsUnauthorized() throws Exception {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 2L, 3L);

        mockMvc.perform(post("/api/replenishment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAllReplenishmentTasks_returnsOkWithList() throws Exception {
        ReplenishmentTaskResponse r1 = new ReplenishmentTaskResponse(1L, 1L, null, 10, ReplenishmentTaskStatus.CREATED, 2L, 3L, null);
        ReplenishmentTaskResponse r2 = new ReplenishmentTaskResponse(2L, 2L, null, 5, ReplenishmentTaskStatus.ASSIGNED, 4L, 5L, null);

        when(replenishmentTaskService.getAllReplenishmentTasks()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/replenishment-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser
    void getAllReplenishmentTasks_emptyList_returnsOkWithEmptyArray() throws Exception {
        when(replenishmentTaskService.getAllReplenishmentTasks()).thenReturn(List.of());

        mockMvc.perform(get("/api/replenishment-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void getReplenishmentTaskById_existingId_returnsOk() throws Exception {
        ReplenishmentTaskResponse response = new ReplenishmentTaskResponse(1L, 1L, null, 10, ReplenishmentTaskStatus.CREATED, 2L, 3L, null);

        when(replenishmentTaskService.getReplenishmentTaskById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/replenishment-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void getReplenishmentTaskById_notFound_returnsNotFound() throws Exception {
        when(replenishmentTaskService.getReplenishmentTaskById(99L))
                .thenThrow(new ReplenishmentTaskNotFoundException(99L));

        mockMvc.perform(get("/api/replenishment-tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateReplenishmentTask_validRequest_returnsOk() throws Exception {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 2L, 15, ReplenishmentTaskStatus.ASSIGNED, 2L, 3L);
        ReplenishmentTaskResponse response = new ReplenishmentTaskResponse(1L, 1L, 2L, 15, ReplenishmentTaskStatus.ASSIGNED, 2L, 3L, null);

        when(replenishmentTaskService.updateReplenishmentTask(eq(1L), any(ReplenishmentTaskUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/replenishment-tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @WithMockUser
    void updateReplenishmentTask_notFound_returnsNotFound() throws Exception {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 2L, 15, ReplenishmentTaskStatus.ASSIGNED, 2L, 3L);

        when(replenishmentTaskService.updateReplenishmentTask(eq(99L), any(ReplenishmentTaskUpdateRequest.class)))
                .thenThrow(new ReplenishmentTaskNotFoundException(99L));

        mockMvc.perform(put("/api/replenishment-tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void assignReplenishmentTask_withOperatorRole_returnsOk() throws Exception {
        ReplenishmentTaskResponse response = new ReplenishmentTaskResponse(1L, 1L, 5L, 10, ReplenishmentTaskStatus.ASSIGNED, 2L, 3L, null);

        when(replenishmentTaskService.assignReplenishmentTask(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/replenishment-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.operatorId").value(5L));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void assignReplenishmentTask_withSupervisorRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/replenishment-tasks/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignReplenishmentTask_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/replenishment-tasks/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void assignReplenishmentTask_notFound_returnsNotFound() throws Exception {
        when(replenishmentTaskService.assignReplenishmentTask(99L))
                .thenThrow(new ReplenishmentTaskNotFoundException(99L));

        mockMvc.perform(patch("/api/replenishment-tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteReplenishmentTask_existingId_returnsNoContent() throws Exception {
        doNothing().when(replenishmentTaskService).deleteReplenishmentTask(1L);

        mockMvc.perform(delete("/api/replenishment-tasks/1"))
                .andExpect(status().isNoContent());

        verify(replenishmentTaskService).deleteReplenishmentTask(1L);
    }

    @Test
    @WithMockUser
    void deleteReplenishmentTask_notFound_returnsNotFound() throws Exception {
        doThrow(new ReplenishmentTaskNotFoundException(99L))
                .when(replenishmentTaskService).deleteReplenishmentTask(99L);

        mockMvc.perform(delete("/api/replenishment-tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void searchReplenishmentTasks_returnsMatchingList() throws Exception {
        ReplenishmentTaskSearchRequest request = new ReplenishmentTaskSearchRequest(1L, null, null, ReplenishmentTaskStatus.CREATED, null, null);
        ReplenishmentTaskResponse response = new ReplenishmentTaskResponse(1L, 1L, null, 10, ReplenishmentTaskStatus.CREATED, 2L, 3L, null);

        when(replenishmentTaskService.searchReplenishmentTasks(any(ReplenishmentTaskSearchRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/replenishment-tasks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    @WithMockUser
    void searchReplenishmentTasks_noMatches_returnsEmptyList() throws Exception {
        ReplenishmentTaskSearchRequest request = new ReplenishmentTaskSearchRequest(999L, null, null, null, null, null);

        when(replenishmentTaskService.searchReplenishmentTasks(any(ReplenishmentTaskSearchRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/replenishment-tasks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        public ReplenishmentTaskService replenishmentTaskService() {
            return mock(ReplenishmentTaskService.class);
        }

        @Bean
        public ReplenishmentTaskController replenishmentTaskController(ReplenishmentTaskService replenishmentTaskService) {
            return new ReplenishmentTaskController(replenishmentTaskService);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}