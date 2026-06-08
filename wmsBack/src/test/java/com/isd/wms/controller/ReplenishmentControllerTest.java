//package com.isd.wms.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
//import com.isd.wms.dto.replenishment.ReplenishmentResponse;
//import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
//import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
//import com.isd.wms.enums.ReplenishmentStatus;
//import com.isd.wms.exception.ReplenishmentNotFoundException;
//import com.isd.wms.service.ReplenishmentService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.MediaType;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringJUnitWebConfig
//@ContextConfiguration(classes = ReplenishmentControllerTest.TestConfig.class)
//class ReplenishmentControllerTest {
//
//    private MockMvc mockMvc;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    @Autowired
//    private ReplenishmentService replenishmentService;
//
//    @BeforeEach
//    void setUp() {
//        reset(replenishmentService);
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
//                .apply(springSecurity())
//                .build();
//    }
//
//    @Test
//    @WithMockUser(roles = "SUPERVISOR")
//    void createReplenishmentTask_withSupervisorRole_returnsCreated() throws Exception {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 2L, 3L);
//        ReplenishmentResponse response = new ReplenishmentResponse(1L, 1L, null, 10, ReplenishmentStatus.CREATED, 2L, 3L, null);
//
//        when(replenishmentService.createReplenishment(any(ReplenishmentCreateRequest.class)))
//                .thenReturn(response);
//
//        mockMvc.perform(post("/api/replenishment-tasks")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(1L))
//                .andExpect(jsonPath("$.status").value("CREATED"));
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void createReplenishmentTask_withOperatorRole_returnsForbidden() throws Exception {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 2L, 3L);
//
//        mockMvc.perform(post("/api/replenishment-tasks")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    void createReplenishmentTask_unauthenticated_returnsUnauthorized() throws Exception {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 2L, 3L);
//
//        mockMvc.perform(post("/api/replenishment-tasks")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser
//    void getAllReplenishmentTasks_returnsOkWithList() throws Exception {
//        ReplenishmentResponse r1 = new ReplenishmentResponse(1L, 1L, null, 10, ReplenishmentStatus.CREATED, 2L, 3L, null);
//        ReplenishmentResponse r2 = new ReplenishmentResponse(2L, 2L, null, 5, ReplenishmentStatus.ASSIGNED, 4L, 5L, null);
//
//        when(replenishmentService.getAllReplenishments()).thenReturn(List.of(r1, r2));
//
//        mockMvc.perform(get("/api/replenishment-tasks"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2))
//                .andExpect(jsonPath("$[0].id").value(1L))
//                .andExpect(jsonPath("$[1].id").value(2L));
//    }
//
//    @Test
//    @WithMockUser
//    void getAllReplenishmentTasks_emptyList_returnsOkWithEmptyArray() throws Exception {
//        when(replenishmentService.getAllReplenishments()).thenReturn(List.of());
//
//        mockMvc.perform(get("/api/replenishment-tasks"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//    }
//
//    @Test
//    @WithMockUser
//    void getReplenishmentTaskById_existingId_returnsOk() throws Exception {
//        ReplenishmentResponse response = new ReplenishmentResponse(1L, 1L, null, 10, ReplenishmentStatus.CREATED, 2L, 3L, null);
//
//        when(replenishmentService.getReplenishmentById(1L)).thenReturn(response);
//
//        mockMvc.perform(get("/api/replenishment-tasks/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1L));
//    }
//
//    @Test
//    @WithMockUser
//    void getReplenishmentTaskById_notFound_returnsNotFound() throws Exception {
//        when(replenishmentService.getReplenishmentById(99L))
//                .thenThrow(new ReplenishmentNotFoundException(99L));
//
//        mockMvc.perform(get("/api/replenishment-tasks/99"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @WithMockUser
//    void updateReplenishmentTask_validRequest_returnsOk() throws Exception {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 2L, 15, ReplenishmentStatus.ASSIGNED, 2L, 3L);
//        ReplenishmentResponse response = new ReplenishmentResponse(1L, 1L, 2L, 15, ReplenishmentStatus.ASSIGNED, 2L, 3L, null);
//
//        when(replenishmentService.updateReplenishment(eq(1L), any(ReplenishmentUpdateRequest.class)))
//                .thenReturn(response);
//
//        mockMvc.perform(put("/api/replenishment-tasks/1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1L))
//                .andExpect(jsonPath("$.status").value("ASSIGNED"));
//    }
//
//    @Test
//    @WithMockUser
//    void updateReplenishmentTask_notFound_returnsNotFound() throws Exception {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 2L, 15, ReplenishmentStatus.ASSIGNED, 2L, 3L);
//
//        when(replenishmentService.updateReplenishment(eq(99L), any(ReplenishmentUpdateRequest.class)))
//                .thenThrow(new ReplenishmentNotFoundException(99L));
//
//        mockMvc.perform(put("/api/replenishment-tasks/99")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void assignReplenishmentTask_withOperatorRole_returnsOk() throws Exception {
//        ReplenishmentResponse response = new ReplenishmentResponse(1L, 1L, 5L, 10, ReplenishmentStatus.ASSIGNED, 2L, 3L, null);
//
//        when(replenishmentService.assignReplenishment(1L)).thenReturn(response);
//
//        mockMvc.perform(patch("/api/replenishment-tasks/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("ASSIGNED"))
//                .andExpect(jsonPath("$.operatorId").value(5L));
//    }
//
//    @Test
//    @WithMockUser(roles = "SUPERVISOR")
//    void assignReplenishmentTask_withSupervisorRole_returnsForbidden() throws Exception {
//        mockMvc.perform(patch("/api/replenishment-tasks/1"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    void assignReplenishmentTask_unauthenticated_returnsUnauthorized() throws Exception {
//        mockMvc.perform(patch("/api/replenishment-tasks/1"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void assignReplenishmentTask_notFound_returnsNotFound() throws Exception {
//        when(replenishmentService.assignReplenishment(99L))
//                .thenThrow(new ReplenishmentNotFoundException(99L));
//
//        mockMvc.perform(patch("/api/replenishment-tasks/99"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @WithMockUser
//    void deleteReplenishmentTask_existingId_returnsNoContent() throws Exception {
//        doNothing().when(replenishmentService).deleteReplenishment(1L);
//
//        mockMvc.perform(delete("/api/replenishment-tasks/1"))
//                .andExpect(status().isNoContent());
//
//        verify(replenishmentService).deleteReplenishment(1L);
//    }
//
//    @Test
//    @WithMockUser
//    void deleteReplenishmentTask_notFound_returnsNotFound() throws Exception {
//        doThrow(new ReplenishmentNotFoundException(99L))
//                .when(replenishmentService).deleteReplenishment(99L);
//
//        mockMvc.perform(delete("/api/replenishment-tasks/99"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @WithMockUser
//    void searchReplenishmentTasks_returnsMatchingList() throws Exception {
//        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(1L, null, null, ReplenishmentStatus.CREATED, null, null);
//        ReplenishmentResponse response = new ReplenishmentResponse(1L, 1L, null, 10, ReplenishmentStatus.CREATED, 2L, 3L, null);
//
//        when(replenishmentService.searchReplenishments(any(ReplenishmentSearchRequest.class)))
//                .thenReturn(List.of(response));
//
//        mockMvc.perform(get("/api/replenishment-tasks/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].status").value("CREATED"));
//    }
//
//    @Test
//    @WithMockUser
//    void searchReplenishmentTasks_noMatches_returnsEmptyList() throws Exception {
//        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(999L, null, null, null, null, null);
//
//        when(replenishmentService.searchReplenishments(any(ReplenishmentSearchRequest.class)))
//                .thenReturn(List.of());
//
//        mockMvc.perform(get("/api/replenishment-tasks/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//    }
//
//    @Configuration
//    @EnableWebMvc
//    @EnableWebSecurity
//    @EnableMethodSecurity
//    static class TestConfig {
//
//        @Bean
//        public ReplenishmentService replenishmentTaskService() {
//            return mock(ReplenishmentService.class);
//        }
//
//        @Bean
//        public ReplenishmentTaskController replenishmentTaskController(ReplenishmentService replenishmentService) {
//            return new ReplenishmentTaskController(replenishmentService);
//        }
//
//        @Bean
//        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//            http
//                    .csrf(csrf -> csrf.disable())
//                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
//            return http.build();
//        }
//    }
//}