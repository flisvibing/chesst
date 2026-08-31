package com.chesst.auth;

import com.chesst.ChesstApplication;
import com.chesst.MockMailConfig;
import com.chesst.auth.dto.LoginRequest;
import com.chesst.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ChesstApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MockMailConfig.class)
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void register_thenLogin_withoutVerification_fails() throws Exception {
        RegisterRequest reg = new RegisterRequest("alice", "alice@example.com", "secret123", "secret123");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("alice", "secret123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordMismatch_rejected() throws Exception {
        RegisterRequest reg = new RegisterRequest("bob", "bob@example.com", "secret123", "different");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateUsername_rejected() throws Exception {
        RegisterRequest reg = new RegisterRequest("carol", "carol@example.com", "secret123", "secret123");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        RegisterRequest dup = new RegisterRequest("carol", "carol2@example.com", "secret123", "secret123");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dup)))
                .andExpect(status().isBadRequest());
    }
}
