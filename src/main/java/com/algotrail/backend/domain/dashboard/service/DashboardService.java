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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final GithubSyncLogRepository githubSyncLogRepository;

    public DashboardResponse getDashboard(Long userId) {
        LocalDate today = LocalDate.now();

        long totalSolvedCount = solvedProblemRepository.countByUserId(userId);
        long todaySolvedCount = solvedProblemRepository.countByUserIdAndSolvedDate(userId, today);
        long reviewCompletedCount =
                reviewScheduleRepository.countBySolvedProblemUserIdAndStatus(userId, "COMPLETED");

        List<ReviewSchedule> todayReviews =
                reviewScheduleRepository.findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        today,
                        "PENDING"
                );

        List<SolvedProblem> recentSolvedProblems =
                solvedProblemRepository.findTop5ByUserIdOrderBySolvedDateDesc(userId);

        List<LocalDate> solvedDates =
                solvedProblemRepository.findDistinctSolvedDatesByUserIdOrderBySolvedDateDesc(userId);

        int currentStreak = calculateCurrentStreak(solvedDates);
        int maxStreak = calculateMaxStreak(solvedDates);

        GithubSyncLog lastSyncLog = githubSyncLogRepository
                .findTopByUserIdOrderBySyncStartedAtDesc(userId)
                .orElse(null);

        int totalTodayTasks = (int) (todaySolvedCount + todayReviews.size());
        int completedTodayTasks = (int) todaySolvedCount;

        int todayCompletionRate = totalTodayTasks == 0
                ? 0
                : (int) Math.round((completedTodayTasks * 100.0) / totalTodayTasks);

        return new DashboardResponse(
                today,
                totalSolvedCount,
                todaySolvedCount,
                reviewCompletedCount,
                todayCompletionRate,
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

    private int calculateCurrentStreak(List<LocalDate> solvedDates) {
        if (solvedDates == null || solvedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate cursor = today;

        if (!solvedDates.contains(today)) {
            cursor = today.minusDays(1);
        }

        int streak = 0;

        while (solvedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

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
            LocalDate previous = sortedDates.get(i - 1);
            LocalDate current = sortedDates.get(i);

            if (current.equals(previous.plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }
}