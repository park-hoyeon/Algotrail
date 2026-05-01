package com.algotrail.backend.domain.problem.dto;

import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;

import java.time.LocalDate;
import java.util.List;

public record ProblemListResponse(
        Long solvedProblemId,
        Long problemId,
        String title,
        String platform,
        String level,
        List<String> categories,
        LocalDate solvedDate,
        String status,
        String language,
        String githubUrl
) {
    public static ProblemListResponse of(
            SolvedProblem solvedProblem,
            List<ProblemCategory> problemCategories
    ) {
        return new ProblemListResponse(
                solvedProblem.getId(),
                solvedProblem.getProblem().getId(),
                solvedProblem.getProblem().getTitle(),
                solvedProblem.getProblem().getPlatform(),
                solvedProblem.getProblem().getLevel(),
                problemCategories.stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList(),
                solvedProblem.getSolvedDate(),
                solvedProblem.getStatus(),
                solvedProblem.getLanguage(),
                solvedProblem.getGithubUrl()
        );
    }
}