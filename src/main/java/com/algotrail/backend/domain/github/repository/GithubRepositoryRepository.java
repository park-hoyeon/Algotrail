package com.algotrail.backend.domain.github.repository;

import com.algotrail.backend.domain.github.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, Long> {

    Optional<GithubRepository> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}