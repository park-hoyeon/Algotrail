package com.algotrail.backend.domain.github.scheduler;

import com.algotrail.backend.domain.github.service.GithubSyncService;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GithubSyncScheduler {

    private final UserRepository userRepository;
    private final GithubSyncService githubSyncService;

    @Scheduled(fixedDelay = 1000 * 60 * 30)
    public void syncAllUsers() {
        log.info("[GitHub Sync Scheduler] 자동 동기화 시작");

        for (User user : userRepository.findAll()) {
            try {
                var result = githubSyncService.sync(user.getId());

                log.info(
                        "[GitHub Sync Scheduler] userId={}, newSolvedCount={}",
                        result.userId(),
                        result.newSolvedCount()
                );
            } catch (Exception e) {
                log.error(
                        "[GitHub Sync Scheduler] userId={} 동기화 실패: {}",
                        user.getId(),
                        e.getMessage()
                );
            }
        }

        log.info("[GitHub Sync Scheduler] 자동 동기화 종료");
    }
}