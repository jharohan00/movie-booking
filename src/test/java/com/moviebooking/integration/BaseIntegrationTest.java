package com.moviebooking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    protected String adminToken;
    protected String customerToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        adminToken = login("admin@moviebooking.com", "Admin@123");
        // Register a fresh customer for each test
        String uniqueEmail = "customer_" + System.currentTimeMillis() + "@test.com";
        register(uniqueEmail, "Customer@123", "Test Customer");
        customerToken = login(uniqueEmail, "Customer@123");
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        var tree = objectMapper.readTree(result.getResponse().getContentAsString());
        return tree.path("data").path("token").asText();
    }

    protected void register(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password, "fullName", fullName))))
                .andExpect(status().isCreated());
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }

    protected <T> String json(T obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
