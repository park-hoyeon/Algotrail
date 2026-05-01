package com.algotrail.backend.domain.user.controller;

import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/github")
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {

        User user = userService.createUser(
                request.username(),
                request.githubUsername(),
                request.githubRepo()
        );

        return new CreateUserResponse(
                user.getId(),
                user.getUsername(),
                user.getGithubUsername(),
                user.getGithubRepo()
        );
    }

    public record CreateUserRequest(
            String username,
            String githubUsername,
            String githubRepo
    ) {}

    public record CreateUserResponse(
            Long userId,
            String username,
            String githubUsername,
            String githubRepo
    ) {}
}