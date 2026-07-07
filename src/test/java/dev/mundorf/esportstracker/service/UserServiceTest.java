package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserByUsername() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findByUsername("testuser");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
