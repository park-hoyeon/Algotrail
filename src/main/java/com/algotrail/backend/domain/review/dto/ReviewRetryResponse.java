package com.algotrail.backend.domain.review.dto;

public record ReviewRetryResponse(
        Long reviewScheduleId,
        Long solvedProblemId,
        String solvedProblemStatus,
        String message
) {
}