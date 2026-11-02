
        package com.cleancity.cleancity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // Test 1: Unauthenticated user should get 401
    @Test
    void unauthenticatedUser_shouldGet401ForAdminApi() throws Exception {

        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized());
    }

    // Test 2: Citizen should not access admin API
    @Test
    void citizen_shouldGet403ForAdminApi() throws Exception {

        mockMvc.perform(
                get("/api/admin/test")
                        .with(SecurityMockMvcRequestPostProcessors
                                .user("citizen@example.com")
                                .roles("CITIZEN"))
        ).andExpect(status().isForbidden());
    }

    // Test 3: Worker should not access citizen API
    @Test
    void worker_shouldGet403ForCitizenApi() throws Exception {

        mockMvc.perform(
                get("/api/citizen/complaints")
                        .with(SecurityMockMvcRequestPostProcessors
                                .user("worker@example.com")
                                .roles("WORKER"))
        ).andExpect(status().isForbidden());
    }

    // Test 4: Admin should pass authorization
    @Test
    void admin_shouldPassAuthorizationForAdminApi() throws Exception {

        mockMvc.perform(
                get("/api/admin/test")
                        .with(SecurityMockMvcRequestPostProcessors
                                .user("admin@example.com")
                                .roles("ADMIN"))
        ).andExpect(status().isNotFound());
    }

    // Test 5: Worker should pass authorization
    @Test
    void worker_shouldPassAuthorizationForWorkerApi() throws Exception {

        mockMvc.perform(
                get("/api/worker/test")
                        .with(SecurityMockMvcRequestPostProcessors
                                .user("worker@example.com")
                                .roles("WORKER"))
        ).andExpect(status().isNotFound());
    }
}
