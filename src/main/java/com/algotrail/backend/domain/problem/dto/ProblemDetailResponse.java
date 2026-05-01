package com.algotrail.backend.domain.problem.dto;

import com.algotrail.backend.domain.problem.entity.ProblemCategory;
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
        List<CategoryItem> categories,
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
            List<ProblemCategory> problemCategories,
            List<ReviewSchedule> reviewSchedules
    ) {
        return new ProblemDetailResponse(
                solvedProblem.getId(),
                solvedProblem.getProblem().getId(),
                solvedProblem.getProblem().getTitle(),
                solvedProblem.getProblem().getPlatform(),
                solvedProblem.getProblem().getLevel(),
                problemCategories.stream()
                        .map(CategoryItem::from)
                        .toList(),
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

    public record CategoryItem(
            Long categoryId,
            String name
    ) {
        public static CategoryItem from(ProblemCategory problemCategory) {
            return new CategoryItem(
                    problemCategory.getCategory().getId(),
                    problemCategory.getCategory().getName()
            );
        }
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