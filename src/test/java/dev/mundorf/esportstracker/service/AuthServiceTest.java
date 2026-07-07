package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.DuplicateResourceException;
import dev.mundorf.esportstracker.model.dto.AuthResponse;
import dev.mundorf.esportstracker.model.dto.LoginRequest;
import dev.mundorf.esportstracker.model.dto.RegisterRequest;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.UserRepository;
import dev.mundorf.esportstracker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterNewUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(jwtService.generateToken("newuser")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password"); // never the raw password
    }

    @Test
    void shouldRejectRegistrationWithTakenUsername() {
        RegisterRequest request = new RegisterRequest("existing", "new@example.com", "password123");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectRegistrationWithTakenEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "taken@example.com", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginAndReturnToken() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(jwtService.generateToken("testuser")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                eq(new UsernamePasswordAuthenticationToken("testuser", "password123")));
    }

    @Test
    void shouldPropagateAuthenticationFailureOnWrongPassword() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
