package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.*;
import com.algotrail.backend.domain.review.service.ReviewScheduleService;
import com.algotrail.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewScheduleService reviewScheduleService;

    @GetMapping("/today")
    public ReviewTodayResponse getTodayReviews(@RequestParam Long userId) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getTodayReviews(userId)
        );
    }

    @GetMapping("/upcoming")
    public ReviewTodayResponse getUpcomingReviews(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getUpcomingReviews(userId, days)
        );
    }

    @GetMapping("/completed")
    public ReviewTodayResponse getCompletedReviews(@RequestParam Long userId) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getCompletedReviews(userId)
        );
    }

    @PatchMapping("/{reviewScheduleId}/complete")
    public void completeReview(@PathVariable Long reviewScheduleId) {
        reviewService.completeReview(reviewScheduleId);
    }

    @PatchMapping("/{reviewScheduleId}/retry")
    public ReviewRetryResponse retryReview(
            @PathVariable Long reviewScheduleId,
            @RequestBody ReviewRetryRequest request
    ) {
        return reviewService.retryReview(reviewScheduleId, request);
    }

    @GetMapping("/problem/{solvedProblemId}")
    public List<ProblemReviewScheduleResponse> getProblemReviewSchedules(
            @PathVariable Long solvedProblemId
    ) {
        return reviewService.getProblemReviewSchedules(solvedProblemId);
    }

    @PostMapping("/backfill")
    public ReviewBackfillResponse backfillReviewSchedules(
            @RequestBody ReviewBackfillRequest request
    ) {
        int createdProblemCount = reviewScheduleService.backfillReviewSchedulesFromDate(
                request.userId(),
                request.startDate()
        );

        return new ReviewBackfillResponse(
                request.userId(),
                request.startDate(),
                createdProblemCount,
                createdProblemCount * 4,
                request.startDate() + " 이후 풀이 문제부터 복습 일정이 생성되었습니다."
        );
    }

    @DeleteMapping("/cleanup")
    public ReviewCleanupResponse cleanupPendingReviewsBeforeDate(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate
    ) {
        int deletedCount = reviewScheduleService.deletePendingReviewSchedulesBeforeDate(
                userId,
                startDate
        );

        return new ReviewCleanupResponse(
                userId,
                startDate,
                deletedCount,
                startDate + " 이전의 미완료 복습 일정 " + deletedCount + "개를 삭제했습니다."
        );
    }

    @PostMapping("/rebuild")
    public ReviewRebuildResponse rebuildReviewSchedules(
            @RequestBody ReviewBackfillRequest request
    ) {
        ReviewScheduleService.ReviewRebuildResult result =
                reviewScheduleService.rebuildPendingReviewSchedulesFromDate(
                        request.userId(),
                        request.startDate()
                );

        return new ReviewRebuildResponse(
                request.userId(),
                request.startDate(),
                result.deletedCount(),
                result.createdProblemCount(),
                result.createdScheduleCount(),
                request.startDate() + " 이후 풀이 문제부터 복습 일정이 다시 생성되었습니다."
        );
    }
}