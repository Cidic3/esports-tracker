package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.UserMapper;
import dev.mundorf.esportstracker.model.dto.UserResponse;
import dev.mundorf.esportstracker.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userMapper.toResponse(userService.findByUsername(userDetails.getUsername()));
    }
}
