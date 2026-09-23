package com.moviebooking.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_andLoginSuccessfully() throws Exception {
        String email = "newuser_" + System.currentTimeMillis() + "@test.com";

        // Register
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "SecurePass1!",
                                "fullName", "John Doe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andReturn();

        // Login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "SecurePass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void register_failsWithDuplicateEmail() throws Exception {
        String email = "dup_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "SecurePass1!", "fullName", "A")))).andReturn();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "SecurePass1!", "fullName", "A"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_failsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@moviebooking.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_failsWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", "SecurePass1!", "fullName", "A"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_failsWithShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "x@test.com", "password", "short", "fullName", "A"))))
                .andExpect(status().isBadRequest());
    }
}
