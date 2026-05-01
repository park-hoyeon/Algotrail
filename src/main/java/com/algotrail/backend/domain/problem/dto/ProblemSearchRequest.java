package com.algotrail.backend.domain.problem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemSearchRequest {

    private Long userId;
    private String keyword;
    private Long categoryId;
    private String status;
}