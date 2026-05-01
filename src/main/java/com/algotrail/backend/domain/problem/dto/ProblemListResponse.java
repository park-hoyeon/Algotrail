package com.algotrail.backend.domain.problem.dto;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;

import java.time.LocalDate;

public record ProblemListResponse(
        Long solvedProblemId,
        Long problemId,
        String title,
        String platform,
        String level,
        LocalDate solvedDate,
        String status,
        String language,
        String githubUrl
) {
    public static ProblemListResponse from(SolvedProblem solvedProblem) {
        return new ProblemListResponse(
                solvedProblem.getId(),
                solvedProblem.getProblem().getId(),
                solvedProblem.getProblem().getTitle(),
                solvedProblem.getProblem().getPlatform(),
                solvedProblem.getProblem().getLevel(),
                solvedProblem.getSolvedDate(),
                solvedProblem.getStatus(),
                solvedProblem.getLanguage(),
                solvedProblem.getGithubUrl()
        );
    }
}