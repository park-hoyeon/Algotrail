package com.algotrail.backend.domain.problem.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.dto.ProblemDetailResponse;
import com.algotrail.backend.domain.problem.dto.ProblemListResponse;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateRequest;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateResponse;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
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
                solvedProblem.getStatus(),
                solvedProblem.getSolveTimeMinutes(),
                solvedProblem.getMemo(),
                "문제 기록이 수정되었습니다."
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
}