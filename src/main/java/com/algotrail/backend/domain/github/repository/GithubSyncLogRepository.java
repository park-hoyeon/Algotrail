package com.algotrail.backend.domain.github.repository;

import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubSyncLogRepository extends JpaRepository<GithubSyncLog, Long> {

    Optional<GithubSyncLog> findTopByUserIdOrderBySyncStartedAtDesc(Long userId);
}