package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;
import java.util.List;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;

public record ReviewTodayResponse(
        LocalDate today,
        List<ReviewItemResponse> reviews
) {
    public static ReviewTodayResponse of(LocalDate today, List<ReviewSchedule> reviews) {
        return new ReviewTodayResponse(
                today,
                reviews.stream()
                        .map(ReviewItemResponse::from)
                        .toList()
        );
    }

    public record ReviewItemResponse(
            Long reviewScheduleId,
            Long solvedProblemId,
            String title,
            String level,
            String language,
            String githubUrl,
            int reviewRound,
            String reviewDayLabel,
            LocalDate reviewDate,
            String status
    ) {
        public static ReviewItemResponse from(ReviewSchedule reviewSchedule) {
            return new ReviewItemResponse(
                    reviewSchedule.getId(),
                    reviewSchedule.getSolvedProblem().getId(),
                    reviewSchedule.getSolvedProblem().getProblem().getTitle(),
                    reviewSchedule.getSolvedProblem().getProblem().getLevel(),
                    reviewSchedule.getSolvedProblem().getLanguage(),
                    reviewSchedule.getSolvedProblem().getGithubUrl(),
                    reviewSchedule.getReviewRound(),
                    toReviewDayLabel(reviewSchedule.getReviewRound()),
                    reviewSchedule.getReviewDate(),
                    reviewSchedule.getStatus()
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
}