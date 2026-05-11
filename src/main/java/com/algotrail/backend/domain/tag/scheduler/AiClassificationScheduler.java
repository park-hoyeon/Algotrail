package com.algotrail.backend.domain.tag.scheduler;

import com.algotrail.backend.domain.github.service.GithubSyncLockService;
import com.algotrail.backend.domain.tag.service.AiProblemClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiClassificationScheduler {

    private final AiProblemClassificationService aiProblemClassificationService;
    private final GithubSyncLockService githubSyncLockService;

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void classifyPendingProblems() {

        if (githubSyncLockService.isAnySyncRunning()) {
            System.out.println("[Rule Scheduler] GitHub 동기화 진행 중이라 분류 건너뜀");
            return;
        }

        System.out.println("[Rule Scheduler] 미분류 문제 분류 시작");

        aiProblemClassificationService.classifyPendingProblems(50);

        System.out.println("[Rule Scheduler] 미분류 문제 분류 종료");
    }
}