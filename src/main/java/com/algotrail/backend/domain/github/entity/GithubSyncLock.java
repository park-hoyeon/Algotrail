package com.algotrail.backend.domain.github.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class GithubSyncLock {

    @Id
    private Long userId;

    private LocalDateTime lockedAt;

    public GithubSyncLock(Long userId) {
        this.userId = userId;
        this.lockedAt = LocalDateTime.now();
    }

    public GithubSyncLock(Long userId, LocalDateTime lockedAt) {
        this.userId = userId;
        this.lockedAt = lockedAt;
    }
}