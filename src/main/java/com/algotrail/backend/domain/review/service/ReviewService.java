package com.algotrail.backend.domain.review.service;

import com.algotrail.backend.domain.problem.entity.ProblemStatus;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.dto.*;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    public void completeReview(Long reviewScheduleId) {
        ReviewSchedule schedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복습 일정을 찾을 수 없습니다."));

        schedule.complete();

        int nextRound = schedule.getReviewRound() + 1;

        if (nextRound <= 3) {
            int nextDays = switch (nextRound) {
                case 2 -> 7;
                case 3 -> 14;
                default -> throw new IllegalStateException("잘못된 복습 회차입니다.");
            };

            reviewScheduleRepository.save(new ReviewSchedule(
                    schedule.getSolvedProblem(),
                    nextRound,
                    schedule.getSolvedProblem().getSolvedDate().plusDays(nextDays)
            ));
        }
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

    public UpcomingReviewResponse getUpcomingReviews(Long userId, int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);

        List<ReviewSchedule> schedules =
                reviewScheduleRepository
                        .findTop20ByStatusAndReviewDateLessThanEqualOrderByReviewDateAsc(
                                "PENDING",
                                LocalDate.now()
                        );

        List<UpcomingReviewResponse.UpcomingReviewItem> reviews = schedules.stream()
                .map(schedule -> {
                    var solvedProblem = schedule.getSolvedProblem();
                    var problem = solvedProblem.getProblem();

                    return new UpcomingReviewResponse.UpcomingReviewItem(
                            schedule.getId(),
                            solvedProblem.getId(),
                            problem.getTitle(),
                            problem.getLevel(),
                            schedule.getStatus(),
                            schedule.getReviewRound(),
                            schedule.getReviewDate(),
                            solvedProblem.getGithubUrl(),
                            solvedProblem.getLanguage()
                    );
                })
                .toList();

        return new UpcomingReviewResponse(
                startDate,
                endDate,
                reviews
        );
    }

    public List<ProblemReviewScheduleResponse> getProblemReviewSchedules(Long solvedProblemId) {
        return reviewScheduleRepository.findBySolvedProblemIdOrderByReviewRoundAsc(solvedProblemId)
                .stream()
                .map(schedule -> new ProblemReviewScheduleResponse(
                        schedule.getId(),
                        schedule.getReviewRound(),
                        schedule.getReviewDate(),
                        schedule.getStatus(),
                        schedule.getCompletedAt()
                ))
                .toList();
    }
}