package com.algotrail.backend.domain.dashboard.dto;

import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDate today,
        long totalSolvedCount,
        long todaySolvedCount,
        long reviewCompletedCount,
        int todayCompletionRate,
        List<TodayReviewItem> todayReviews,
        List<RecentSolvedItem> recentSolvedProblems,
        LastGithubSyncInfo lastGithubSyncInfo
) {
    public record TodayReviewItem(
            Long reviewScheduleId,
            Long solvedProblemId,
            String title,
            String level,
            String language,
            String githubUrl,
            int reviewRound,
            String reviewDayLabel,
            LocalDate reviewDate
    ) {
        public static TodayReviewItem from(ReviewSchedule reviewSchedule) {
            return new TodayReviewItem(
                    reviewSchedule.getId(),
                    reviewSchedule.getSolvedProblem().getId(),
                    reviewSchedule.getSolvedProblem().getProblem().getTitle(),
                    reviewSchedule.getSolvedProblem().getProblem().getLevel(),
                    reviewSchedule.getSolvedProblem().getLanguage(),
                    reviewSchedule.getSolvedProblem().getGithubUrl(),
                    reviewSchedule.getReviewRound(),
                    toReviewDayLabel(reviewSchedule.getReviewRound()),
                    reviewSchedule.getReviewDate()
            );
        }

        private static String toReviewDayLabel(int reviewRound) {
            return switch (reviewRound) {
                case 1 -> "3일차 복습";
                case 2 -> "7일차 복습";
                case 3 -> "14일차 복습";
                default -> reviewRound + "차 복습";
            };
        }
    }

    public record RecentSolvedItem(
            Long solvedProblemId,
            String title,
            String level,
            String language,
            String status,
            LocalDate solvedDate,
            String githubUrl
    ) {
        public static RecentSolvedItem from(SolvedProblem solvedProblem) {
            return new RecentSolvedItem(
                    solvedProblem.getId(),
                    solvedProblem.getProblem().getTitle(),
                    solvedProblem.getProblem().getLevel(),
                    solvedProblem.getLanguage(),
                    solvedProblem.getStatus(),
                    solvedProblem.getSolvedDate(),
                    solvedProblem.getGithubUrl()
            );
        }
    }

    public record LastGithubSyncInfo(
            LocalDateTime syncStartedAt,
            LocalDateTime syncFinishedAt,
            int addedCount,
            int skippedCount,
            String status,
            String message
    ) {
        public static LastGithubSyncInfo from(GithubSyncLog log) {
            if (log == null) {
                return null;
            }

            return new LastGithubSyncInfo(
                    log.getSyncStartedAt(),
                    log.getSyncFinishedAt(),
                    log.getAddedCount(),
                    log.getSkippedCount(),
                    log.getStatus().name(),
                    log.getMessage()
            );
        }
    }
}