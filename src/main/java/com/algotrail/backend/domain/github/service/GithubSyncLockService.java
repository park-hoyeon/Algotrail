package com.algotrail.backend.domain.github.service;

import com.algotrail.backend.domain.github.entity.GithubSyncLock;
import com.algotrail.backend.domain.github.repository.GithubSyncLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GithubSyncLockService {

    private final GithubSyncLockRepository githubSyncLockRepository;

    @Transactional
    public boolean tryLock(Long userId) {
        if (githubSyncLockRepository.existsById(userId)) {
            return false;
        }

        githubSyncLockRepository.save(new GithubSyncLock(userId, LocalDateTime.now()));
        return true;
    }

    @Transactional
    public void unlock(Long userId) {
        githubSyncLockRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public boolean isAnySyncRunning() {
        return githubSyncLockRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public boolean isSyncRunning(Long userId) {
        return githubSyncLockRepository.existsById(userId);
    }
}