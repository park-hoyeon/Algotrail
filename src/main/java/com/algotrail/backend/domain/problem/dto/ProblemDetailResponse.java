package com.algotrail.backend.domain.problem.dto;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;

import java.time.LocalDate;
import java.util.List;

public record ProblemDetailResponse(
        Long solvedProblemId,
        Long problemId,
        String title,
        String platform,
        String level,
        String language,
        String status,
        String githubUrl,
        LocalDate solvedDate,
        Integer solveTimeMinutes,
        String memo,
        List<ReviewScheduleResponse> reviewSchedules
) {
    public static ProblemDetailResponse of(
            SolvedProblem solvedProblem,
            List<ReviewSchedule> reviewSchedules
    ) {
        return new ProblemDetailResponse(
                solvedProblem.getId(),
                solvedProblem.getProblem().getId(),
                solvedProblem.getProblem().getTitle(),
                solvedProblem.getProblem().getPlatform(),
                solvedProblem.getProblem().getLevel(),
                solvedProblem.getLanguage(),
                solvedProblem.getStatus(),
                solvedProblem.getGithubUrl(),
                solvedProblem.getSolvedDate(),
                solvedProblem.getSolveTimeMinutes(),
                solvedProblem.getMemo(),
                reviewSchedules.stream()
                        .map(ReviewScheduleResponse::from)
                        .toList()
        );
    }

    public record ReviewScheduleResponse(
            Long reviewScheduleId,
            int reviewRound,
            LocalDate reviewDate,
            String status
    ) {
        public static ReviewScheduleResponse from(ReviewSchedule reviewSchedule) {
            return new ReviewScheduleResponse(
                    reviewSchedule.getId(),
                    reviewSchedule.getReviewRound(),
                    reviewSchedule.getReviewDate(),
                    reviewSchedule.getStatus()
            );
        }
    }
}