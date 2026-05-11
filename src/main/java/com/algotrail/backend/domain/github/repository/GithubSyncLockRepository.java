package com.algotrail.backend.domain.github.repository;

import com.algotrail.backend.domain.github.entity.GithubSyncLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubSyncLockRepository extends JpaRepository<GithubSyncLock, Long> {
}