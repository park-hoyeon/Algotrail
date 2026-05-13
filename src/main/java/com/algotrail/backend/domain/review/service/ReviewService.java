package com.algotrail.backend.domain.review.service;

import com.algotrail.backend.domain.problem.entity.ProblemStatus;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.dto.*;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final int OVERDUE_LOOKBACK_DAYS = 60;

    private final ReviewScheduleRepository reviewScheduleRepository;

    public ReviewTodayResponse getTodayReviews(Long userId) {
        LocalDate today = LocalDate.now();

        List<ReviewSchedule> reviews = reviewScheduleRepository
                .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        today,
                        STATUS_PENDING
                );

        return ReviewTodayResponse.of(today, reviews);
    }

    @Transactional
    public ReviewCompleteResponse completeReview(Long reviewScheduleId) {
        ReviewSchedule schedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복습 일정을 찾을 수 없습니다."));

        schedule.complete();

        updateNextReviewDate(schedule);

        return new ReviewCompleteResponse(
                schedule.getId(),
                schedule.getStatus(),
                schedule.getCompletedAt(),
                "복습이 완료되었습니다. 다음 복습 일정이 완료일 기준으로 다시 계산되었습니다."
        );
    }

    private void updateNextReviewDate(ReviewSchedule completedSchedule) {
        int currentRound = completedSchedule.getReviewRound();

        if (currentRound >= 4) {
            return;
        }

        int nextRound = currentRound + 1;

        reviewScheduleRepository
                .findBySolvedProblemIdAndReviewRound(
                        completedSchedule.getSolvedProblem().getId(),
                        nextRound
                )
                .ifPresent(nextSchedule -> {
                    if (STATUS_COMPLETED.equals(nextSchedule.getStatus())) {
                        return;
                    }

                    LocalDate completedDate = completedSchedule.getCompletedAt().toLocalDate();
                    int plusDays = getDaysBetweenRounds(currentRound, nextRound);

                    nextSchedule.reschedule(
                            completedDate,
                            completedDate.plusDays(plusDays)
                    );
                });
    }

    private int getDaysBetweenRounds(int currentRound, int nextRound) {
        return getReviewDayByRound(nextRound) - getReviewDayByRound(currentRound);
    }

    private int getReviewDayByRound(int round) {
        return switch (round) {
            case 1 -> 3;
            case 2 -> 7;
            case 3 -> 14;
            case 4 -> 30;
            default -> throw new IllegalArgumentException("잘못된 복습 차수입니다.");
        };
    }

    @Transactional
    public ReviewRetryResponse retryReview(Long reviewScheduleId, ReviewRetryRequest request) {
        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 복습 일정입니다."));

        SolvedProblem solvedProblem = reviewSchedule.getSolvedProblem();

        solvedProblem.updateStatus(ProblemStatus.RETRY);

        if (request != null && request.memo() != null && !request.memo().isBlank()) {
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
        LocalDate today = LocalDate.now();

        LocalDate startDate = today.minusDays(OVERDUE_LOOKBACK_DAYS);
        LocalDate endDate = today.plusDays(days);

        List<ReviewSchedule> schedules = reviewScheduleRepository.findUpcomingReviewsByUser(
                userId,
                STATUS_PENDING,
                startDate,
                endDate
        );

        List<UpcomingReviewResponse.UpcomingReviewItem> reviews = schedules.stream()
                .map(this::toUpcomingReviewItem)
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

    public List<ReviewSchedule> getCompletedReviews(Long userId) {
        return reviewScheduleRepository.findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
                userId,
                STATUS_COMPLETED
        );
    }

    private UpcomingReviewResponse.UpcomingReviewItem toUpcomingReviewItem(ReviewSchedule schedule) {
        SolvedProblem solvedProblem = schedule.getSolvedProblem();
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
    }
}