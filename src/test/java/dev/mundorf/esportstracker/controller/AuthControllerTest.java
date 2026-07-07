package dev.mundorf.esportstracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mundorf.esportstracker.model.dto.AuthResponse;
import dev.mundorf.esportstracker.model.dto.LoginRequest;
import dev.mundorf.esportstracker.model.dto.RegisterRequest;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/auth/** is public and doesn't touch @AuthenticationPrincipal. JwtAuthenticationFilter is
 * excluded from component scanning here for the same reason as GameControllerTest - it would
 * otherwise be auto-constructed by this slice regardless of addFilters, needing a live
 * UserRepository this test has no way to provide.
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldRegisterAndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");
        when(authService.register(any())).thenReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldRejectRegistrationWithShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "not-an-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(authService.login(any())).thenReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}
