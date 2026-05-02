package com.algotrail.backend.domain.dashboard.service;

import com.algotrail.backend.domain.dashboard.dto.DashboardResponse;
import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import com.algotrail.backend.domain.github.repository.GithubSyncLogRepository;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TODAY_GOAL_COUNT = 10;

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final GithubSyncLogRepository githubSyncLogRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {

        LocalDate today = LocalDate.now();

        long totalSolvedCount =
                solvedProblemRepository.countByUserId(userId);

        long todaySolvedCount =
                solvedProblemRepository.countByUserIdAndSolvedDate(userId, today);

        long reviewCompletedCount =
                reviewScheduleRepository.countBySolvedProblemUserIdAndStatus(
                        userId,
                        "COMPLETED"
                );

        List<ReviewSchedule> todayReviews =
                reviewScheduleRepository
                        .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                                userId,
                                today,
                                "PENDING"
                        );

        List<SolvedProblem> recentSolvedProblems =
                solvedProblemRepository.findTop5ByUserIdOrderBySolvedDateDesc(userId);

        // ✅ 안전한 streak 계산 (문제 풀이 기준)
        List<LocalDate> solvedDates =
                solvedProblemRepository
                        .findDistinctSolvedDatesByUserIdOrderBySolvedDateDesc(userId);

        int currentStreak = calculateCurrentStreak(solvedDates);
        int maxStreak = calculateMaxStreak(solvedDates);

        // 평균 풀이 시간
        LocalDate thirtyDaysAgo = today.minusDays(30);

        Double average =
                solvedProblemRepository
                        .findAverageSolveTimeMinutesByUserIdAfterDate(userId, thirtyDaysAgo);

        int averageSolveTimeMinutes =
                average == null ? 0 : (int) Math.round(average);

        // GitHub sync
        GithubSyncLog lastSyncLog =
                githubSyncLogRepository
                        .findTopByUserIdOrderBySyncStartedAtDesc(userId)
                        .orElse(null);

        int todayCompletionRate = (int) Math.min(
                100,
                Math.round((todaySolvedCount * 100.0) / TODAY_GOAL_COUNT)
        );

        return new DashboardResponse(
                today,
                totalSolvedCount,
                todaySolvedCount,
                TODAY_GOAL_COUNT,
                reviewCompletedCount,
                todayCompletionRate,
                averageSolveTimeMinutes,
                currentStreak,
                maxStreak,
                todayReviews.stream()
                        .map(DashboardResponse.TodayReviewItem::from)
                        .toList(),
                recentSolvedProblems.stream()
                        .map(DashboardResponse.RecentSolvedItem::from)
                        .toList(),
                DashboardResponse.LastGithubSyncInfo.from(lastSyncLog)
        );
    }

    // =========================
    // 현재 연속 학습
    // =========================
    private int calculateCurrentStreak(List<LocalDate> solvedDates) {

        if (solvedDates == null || solvedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        LocalDate cursor =
                solvedDates.contains(today)
                        ? today
                        : today.minusDays(1);

        int streak = 0;

        while (solvedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    // =========================
    // 최대 연속 학습
    // =========================
    private int calculateMaxStreak(List<LocalDate> solvedDates) {

        if (solvedDates == null || solvedDates.isEmpty()) {
            return 0;
        }

        List<LocalDate> sortedDates = solvedDates.stream()
                .sorted()
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {

            LocalDate prev = sortedDates.get(i - 1);
            LocalDate curr = sortedDates.get(i);

            if (curr.equals(prev.plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }
}