package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.DuplicateResourceException;
import dev.mundorf.esportstracker.model.dto.AuthResponse;
import dev.mundorf.esportstracker.model.dto.LoginRequest;
import dev.mundorf.esportstracker.model.dto.RegisterRequest;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.UserRepository;
import dev.mundorf.esportstracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = new User(request.username(), request.email(), passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user.getUsername()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        return new AuthResponse(jwtService.generateToken(request.username()));
    }
}
