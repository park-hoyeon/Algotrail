package com.algotrail.backend.domain.problem.service;

import com.algotrail.backend.domain.problem.dto.ProblemDetailResponse;
import com.algotrail.backend.domain.problem.dto.ProblemListResponse;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateRequest;
import com.algotrail.backend.domain.problem.dto.ProblemUpdateResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;

    public List<ProblemListResponse> getProblems(Long userId) {
        return solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId)
                .stream()
                .map(ProblemListResponse::from)
                .toList();
    }

    public ProblemDetailResponse getProblemDetail(Long solvedProblemId) {
        SolvedProblem solvedProblem = solvedProblemRepository.findById(solvedProblemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 풀이 기록입니다."));

        List<ReviewSchedule> reviewSchedules =
                reviewScheduleRepository.findBySolvedProblemOrderByReviewRoundAsc(solvedProblem);

        return ProblemDetailResponse.of(solvedProblem, reviewSchedules);
    }

    public ProblemUpdateResponse updateProblem(Long solvedProblemId, ProblemUpdateRequest request) {
        SolvedProblem solvedProblem = solvedProblemRepository.findById(solvedProblemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 풀이 기록입니다."));

        solvedProblem.updateInfo(
                request.status(),
                request.solveTimeMinutes(),
                request.memo()
        );

        solvedProblemRepository.save(solvedProblem);

        return new ProblemUpdateResponse(
                solvedProblem.getId(),
                solvedProblem.getStatus(),
                solvedProblem.getSolveTimeMinutes(),
                solvedProblem.getMemo(),
                "문제 기록이 수정되었습니다."
        );
    }
}