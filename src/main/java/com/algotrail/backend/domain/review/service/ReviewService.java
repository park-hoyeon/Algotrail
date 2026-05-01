package com.algotrail.backend.domain.review.service;

import com.algotrail.backend.domain.problem.entity.ProblemStatus;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.dto.ReviewCompleteResponse;
import com.algotrail.backend.domain.review.dto.ReviewRetryRequest;
import com.algotrail.backend.domain.review.dto.ReviewRetryResponse;
import com.algotrail.backend.domain.review.dto.ReviewTodayResponse;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewScheduleRepository reviewScheduleRepository;

    public ReviewTodayResponse getTodayReviews(Long userId) {
        LocalDate today = LocalDate.now();

        var reviews = reviewScheduleRepository
                .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        today,
                        "PENDING"
                );

        return ReviewTodayResponse.of(today, reviews);
    }

    public ReviewCompleteResponse completeReview(Long reviewScheduleId) {
        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 복습 일정입니다."));

        reviewSchedule.complete();
        reviewScheduleRepository.save(reviewSchedule);

        return new ReviewCompleteResponse(
                reviewSchedule.getId(),
                reviewSchedule.getStatus(),
                reviewSchedule.getCompletedAt(),
                "복습이 완료되었습니다."
        );
    }

    public ReviewRetryResponse retryReview(Long reviewScheduleId, ReviewRetryRequest request) {

        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 복습 일정입니다."));

        SolvedProblem solvedProblem = reviewSchedule.getSolvedProblem();

        solvedProblem.updateStatus(ProblemStatus.RETRY);

        if (request.memo() != null && !request.memo().isBlank()) {
            String existingMemo = solvedProblem.getMemo() == null ? "" : solvedProblem.getMemo();

            solvedProblem.updateInfo(
                    solvedProblem.getStatus(),
                    solvedProblem.getSolveTimeMinutes(),
                    existingMemo + "\n[RETRY] " + request.memo()
            );
        }

        return new ReviewRetryResponse(
                reviewSchedule.getId(),
                solvedProblem.getId(),
                solvedProblem.getStatus().name(),
                "다시 풀기 필요 상태로 변경되었습니다."
        );
    }
}