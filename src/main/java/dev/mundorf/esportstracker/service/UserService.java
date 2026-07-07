package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
