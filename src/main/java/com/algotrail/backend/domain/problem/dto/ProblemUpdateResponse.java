package com.algotrail.backend.domain.problem.dto;

public record ProblemUpdateResponse(
        Long solvedProblemId,
        String status,
        Integer solveTimeMinutes,
        String memo
) {
}