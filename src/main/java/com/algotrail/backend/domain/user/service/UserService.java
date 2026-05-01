package com.algotrail.backend.domain.user.service;

import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(String username, String githubUsername, String githubRepo) {
        User user = new User(username, githubUsername, githubRepo);
        return userRepository.save(user);
    }
}