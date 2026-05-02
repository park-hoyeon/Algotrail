package com.algotrail.backend.domain.problem.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.dto.*;
import com.algotrail.backend.domain.problem.entity.*;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final ProblemCategoryRepository problemCategoryRepository;
    private final CategoryRepository categoryRepository;

    public List<ProblemListResponse> getProblems(Long userId) {
        return solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId)
                .stream()
                .map(solvedProblem -> {
                    List<ProblemCategory> categories =
                            problemCategoryRepository.findByProblem(solvedProblem.getProblem());

                    return ProblemListResponse.of(solvedProblem, categories);
                })
                .toList();
    }

    public ProblemDetailResponse getProblemDetail(Long solvedProblemId) {
        SolvedProblem solvedProblem = solvedProblemRepository.findById(solvedProblemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 풀이 기록입니다."));

        List<ProblemCategory> categories =
                problemCategoryRepository.findByProblem(solvedProblem.getProblem());

        List<ReviewSchedule> reviewSchedules =
                reviewScheduleRepository.findBySolvedProblemOrderByReviewRoundAsc(solvedProblem);

        return ProblemDetailResponse.of(solvedProblem, categories, reviewSchedules);
    }

    @Transactional
    public ProblemUpdateResponse updateProblem(Long solvedProblemId, ProblemUpdateRequest request) {
        SolvedProblem solvedProblem = solvedProblemRepository.findById(solvedProblemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 풀이 기록입니다."));

        solvedProblem.updateInfo(
                request.status(),
                request.solveTimeMinutes(),
                request.memo()
        );

        if (request.categoryIds() != null) {
            updateCategories(solvedProblem.getProblem(), request.categoryIds());
        }

        return new ProblemUpdateResponse(
                solvedProblem.getId(),
                solvedProblem.getStatus().name(),
                solvedProblem.getSolveTimeMinutes(),
                solvedProblem.getMemo()
        );
    }

    private void updateCategories(Problem problem, List<Long> categoryIds) {
        problemCategoryRepository.deleteByProblem(problem);

        List<Category> categories = categoryRepository.findAllById(categoryIds);

        List<ProblemCategory> problemCategories = categories.stream()
                .map(category -> new ProblemCategory(problem, category))
                .toList();

        problemCategoryRepository.saveAll(problemCategories);
    }

    public List<ProblemListResponse> searchProblems(
            Long userId,
            String keyword,
            Long categoryId,
            String status
    ) {
        String trimmedKeyword = null;
        if (keyword != null && !keyword.isBlank()) {
            trimmedKeyword = keyword.trim();
        }

        ProblemStatus problemStatus = null;
        if (status != null && !status.isBlank()) {
            problemStatus = ProblemStatus.valueOf(status.trim().toUpperCase());
        }

        return solvedProblemRepository.searchProblems(
                        userId,
                        trimmedKeyword,
                        categoryId,
                        problemStatus
                )
                .stream()
                .map(solvedProblem -> {
                    List<ProblemCategory> categories =
                            problemCategoryRepository.findByProblem(solvedProblem.getProblem());

                    return ProblemListResponse.of(solvedProblem, categories);
                })
                .toList();
    }

    @Transactional
    public ProblemCategoryUpdateResponse updateProblemCategory(
            Long solvedProblemId,
            ProblemCategoryUpdateRequest request
    ) {
        SolvedProblem solvedProblem = solvedProblemRepository.findById(solvedProblemId)
                .orElseThrow(() -> new IllegalArgumentException("풀이 기록을 찾을 수 없습니다."));

        Problem problem = solvedProblem.getProblem();

        Category category = categoryRepository.findByName(request.categoryName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + request.categoryName()));

        problemCategoryRepository.deleteByProblem(problem);

        problemCategoryRepository.save(new ProblemCategory(problem, category));

        return new ProblemCategoryUpdateResponse(
                solvedProblem.getId(),
                problem.getId(),
                category.getName(),
                "문제 유형이 수정되었습니다."
        );
    }
}