package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.UserMapper;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unlike GameControllerTest/AuthControllerTest, this one keeps the security filter chain ENABLED
 * (no addFilters = false) because /api/users/me actually depends on an authenticated principal via
 * @AuthenticationPrincipal - disabling filters would let an unauthenticated request reach the
 * controller and NPE on a null principal instead of being rejected the way it really would be.
 * We don't import the app's real SecurityConfig either (that would need a live UserRepository for
 * CustomUserDetailsService); @WebMvcTest's default auto-configured security is enough to prove
 * "authenticated -> 200" and "unauthenticated -> rejected". The exact 401 status code from our
 * custom AuthenticationEntryPoint was already verified against the real running app earlier.
 *
 * JwtAuthenticationFilter is excluded from this slice's component scan: as a @Component
 * implementing Filter, @WebMvcTest auto-detects and instantiates it, which would otherwise pull in
 * its real dependencies (JwtService, CustomUserDetailsService -> UserRepository) that this test
 * slice has no way to provide.
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(UserMapper.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCurrentUserProfile() throws Exception {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userService.findByUsername("testuser")).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }
}
