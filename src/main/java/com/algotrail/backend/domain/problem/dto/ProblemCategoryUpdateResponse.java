package com.algotrail.backend.domain.problem.dto;

public record ProblemCategoryUpdateResponse(
        Long solvedProblemId,
        Long problemId,
        String categoryName,
        String message
) {
}