package com.algotrail.backend.domain.user.repository;

import com.algotrail.backend.domain.user.entity.LoginProvider;
import com.algotrail.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(LoginProvider provider, String providerId);
}