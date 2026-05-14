package com.algotrail.backend.domain.user.controller;

import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getProvider().name(),
                user.getGithubUsername(),
                user.getGithubRepo()
        );
    }

    public record UserResponse(
            Long userId,
            String username,
            String email,
            String profileImageUrl,
            String provider,
            String githubUsername,
            String githubRepo
    ) {}
}