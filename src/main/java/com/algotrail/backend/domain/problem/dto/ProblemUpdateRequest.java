package com.algotrail.backend.domain.problem.dto;

import java.util.List;

public record ProblemUpdateRequest(
        String status,
        Integer solveTimeMinutes,
        String memo,
        List<Long> categoryIds
) {
}