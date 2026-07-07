package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.UserResponse;
import dev.mundorf.esportstracker.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
