package com.algotrail.backend.domain.problem.dto;

import com.algotrail.backend.domain.problem.entity.ProblemStatus;

import java.util.List;

public record ProblemUpdateRequest(
        ProblemStatus status,
        Integer solveTimeMinutes,
        String memo,
        List<Long> categoryIds
) {
}