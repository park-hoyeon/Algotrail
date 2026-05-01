package com.algotrail.backend.domain.problem.dto;

public record ProblemUpdateRequest(
        String status,
        Integer solveTimeMinutes,
        String memo
) {
}