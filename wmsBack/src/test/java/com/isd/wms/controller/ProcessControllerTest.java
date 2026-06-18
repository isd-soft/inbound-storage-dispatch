//package com.isd.wms.controller;
//
//import com.isd.wms.dto.allocation.ProcessOperatorResponse;
//import com.isd.wms.enums.Status;
//import com.isd.wms.service.AllocationExecutionService;
//import com.isd.wms.service.AllocationService;
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
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.reset;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringJUnitWebConfig
//@ContextConfiguration(classes = AllocationControllerTest.TestConfig.class)
//class AllocationControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    @Autowired
//    private AllocationService allocationService;
//
//    private ProcessOperatorResponse mockResponse;
//
//    @BeforeEach
//    void setUp() {
//        reset(allocationService);
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
//                .apply(springSecurity())
//                .build();
//
//        mockResponse = new ProcessOperatorResponse(
//                50L,
//                100L,
//                10L,
//                "Test Product",
//                "ZONE-A",
//                10,
//                Status.CREATED
//        );
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void getAvailableProcesses_ShouldReturn200AndList() throws Exception {
//        when(allocationService.getAvailableProcesses()).thenReturn(List.of(mockResponse));
//
//        mockMvc.perform(get("/api/processes/available")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].id").value(50L))
//                .andExpect(jsonPath("$[0].productName").value("Test Product"));
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void getMyProcesses_ShouldReturn200AndList() throws Exception {
//        ProcessOperatorResponse assignedResponse = new ProcessOperatorResponse(50L, 100L, 10L, "Test Product", "ZONE-A", 10, Status.ASSIGNED);
//        when(allocationService.getMyProcesses()).thenReturn(List.of(assignedResponse));
//
//        mockMvc.perform(get("/api/processes/my")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void assignProcess_ShouldReturn200AndAssignedProcess() throws Exception {
//        ProcessOperatorResponse assignedResponse = new ProcessOperatorResponse(50L, 100L, 10L, "Test Product", "ZONE-A", 10, Status.ASSIGNED);
//        when(allocationService.assignProcess(anyLong())).thenReturn(assignedResponse);
//
//        mockMvc.perform(patch("/api/processes/50/assign")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("ASSIGNED"));
//    }
//
//    @Test
//    @WithMockUser(roles = "OPERATOR")
//    void completeProcess_ShouldReturn200AndCompletedProcess() throws Exception {
//        ProcessOperatorResponse completedResponse = new ProcessOperatorResponse(50L, 100L, 10L, "Test Product", "ZONE-A", 10, Status.COMPLETED);
//        when(allocationService.completeProcess(anyLong())).thenReturn(completedResponse);
//
//        mockMvc.perform(patch("/api/processes/50/complete")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("COMPLETED"));
//    }
//
//    @Test
//    @WithMockUser(roles = "USER")
//    void getAvailableProcesses_WithWrongRole_ShouldReturn403Forbidden() throws Exception {
//        mockMvc.perform(get("/api/processes/available")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Configuration
//    @EnableWebMvc
//    @EnableWebSecurity
//    @EnableMethodSecurity
//    static class TestConfig {
//
//        @Bean
//        public AllocationService allocationService() {
//            return mock(allocationService.class);
//        }
//
//        @Bean
//        public AllocationExecutionService allocationExecutionService() {
//            return mock(allocationExecutionService.class);
//        }
//
//        @Bean
//        public AllocationController AllocationController(AllocationService allocationService, AllocationExecutionService allocationExecutionService) {
//            return new AllocationController(allocationService, allocationExecutionService);
//        }
//
//        @Bean
//        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//            http.csrf(csrf -> csrf.disable())
//                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
//            return http.build();
//        }
//    }
//}
