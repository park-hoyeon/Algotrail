package com.algotrail.backend.domain.user.service;

import com.algotrail.backend.domain.user.entity.LoginProvider;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createOAuthUser(
            String username,
            String email,
            String profileImageUrl,
            LoginProvider provider,
            String providerId
    ) {

        User user = new User(
                username,
                email,
                profileImageUrl,
                provider,
                providerId
        );

        return userRepository.save(user);
    }
}