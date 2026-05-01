package com.algotrail.backend.domain.problem.controller;

import com.algotrail.backend.domain.problem.dto.ProblemDetailResponse;
import com.algotrail.backend.domain.problem.dto.ProblemListResponse;
import com.algotrail.backend.domain.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateRequest;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateResponse;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public List<ProblemListResponse> getProblems(@RequestParam Long userId) {
        return problemService.getProblems(userId);
    }

    @GetMapping("/search")
    public List<ProblemListResponse> searchProblems(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status
    ) {
        return problemService.searchProblems(userId, keyword, categoryId, status);
    }

    @GetMapping("/{solvedProblemId}")
    public ProblemDetailResponse getProblemDetail(@PathVariable Long solvedProblemId) {
        return problemService.getProblemDetail(solvedProblemId);
    }

    @PatchMapping("/{solvedProblemId}")
    public ProblemUpdateResponse updateProblem(
            @PathVariable Long solvedProblemId,
            @RequestBody ProblemUpdateRequest request
    ) {
        return problemService.updateProblem(solvedProblemId, request);
    }
}