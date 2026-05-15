package com.algotrail.backend.domain.review.service;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.dto.ReviewCompleteResponse;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewScheduleService {

    private static final int[] REVIEW_DAYS = {3, 7, 14, 30};
    private static final int OVERDUE_LOOKBACK_DAYS = 60;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ReviewScheduleRepository reviewScheduleRepository;
    private final SolvedProblemRepository solvedProblemRepository;

    @Transactional
    public void createReviewSchedules(SolvedProblem solvedProblem) {
        if (reviewScheduleRepository.existsBySolvedProblemId(solvedProblem.getId())) {
            return;
        }

        LocalDate baseDate = getSolvedBaseDate(solvedProblem);
        createReviewSchedulesByBaseDate(solvedProblem, baseDate);
    }

    @Transactional
    public int backfillReviewSchedules(Long userId) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId);

        return createReviewSchedulesForProblems(solvedProblems);
    }

    @Transactional
    public int backfillReviewSchedulesFromDate(Long userId, LocalDate startDate) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateGreaterThanEqualOrderBySolvedDateDesc(
                        userId,
                        startDate
                );

        return createReviewSchedulesForProblems(solvedProblems);
    }

    @Transactional
    public ReviewRebuildResult rebuildPendingReviewSchedulesFromDate(
            Long userId,
            LocalDate startDate
    ) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId);

        int deletedCount = 0;
        int createdScheduleCount = 0;
        int affectedProblemCount = 0;

        for (SolvedProblem solvedProblem : solvedProblems) {
            List<ReviewSchedule> schedules =
                    reviewScheduleRepository.findBySolvedProblemIdOrderByReviewRoundAsc(
                            solvedProblem.getId()
                    );

            boolean hasCompletedSchedule = schedules.stream()
                    .anyMatch(schedule -> STATUS_COMPLETED.equals(schedule.getStatus()));

            boolean isTargetProblem =
                    isSolvedAfterOrOn(solvedProblem, startDate) || hasCompletedSchedule;

            if (!isTargetProblem) {
                continue;
            }

            List<ReviewSchedule> pendingSchedules = schedules.stream()
                    .filter(schedule -> STATUS_PENDING.equals(schedule.getStatus()))
                    .toList();

            deletedCount += pendingSchedules.size();
            reviewScheduleRepository.deleteAll(pendingSchedules);

            int createdForProblem = recreateOnlyNotCompletedSchedules(
                    solvedProblem,
                    schedules,
                    startDate
            );

            if (createdForProblem > 0 || !pendingSchedules.isEmpty()) {
                affectedProblemCount++;
            }

            createdScheduleCount += createdForProblem;
        }

        return new ReviewRebuildResult(
                deletedCount,
                affectedProblemCount,
                createdScheduleCount
        );
    }

    private boolean isSolvedAfterOrOn(SolvedProblem solvedProblem, LocalDate startDate) {
        LocalDate solvedDate = getSolvedBaseDate(solvedProblem);
        return !solvedDate.isBefore(startDate);
    }

    private int recreateOnlyNotCompletedSchedules(
            SolvedProblem solvedProblem,
            List<ReviewSchedule> oldSchedules,
            LocalDate startDate
    ) {
        Set<Integer> completedRounds = new HashSet<>();

        ReviewSchedule latestCompletedSchedule = null;

        for (ReviewSchedule schedule : oldSchedules) {
            if (!STATUS_COMPLETED.equals(schedule.getStatus())) {
                continue;
            }

            completedRounds.add(schedule.getReviewRound());

            if (latestCompletedSchedule == null ||
                    schedule.getReviewRound() > latestCompletedSchedule.getReviewRound()) {
                latestCompletedSchedule = schedule;
            }
        }

        int createdCount = 0;

        for (int i = 0; i < REVIEW_DAYS.length; i++) {
            int round = i + 1;

            if (completedRounds.contains(round)) {
                continue;
            }

            boolean exists = reviewScheduleRepository.existsBySolvedProblemIdAndReviewRound(
                    solvedProblem.getId(),
                    round
            );

            if (exists) {
                continue;
            }

            LocalDate baseDate;
            LocalDate reviewDate;

            if (latestCompletedSchedule != null &&
                    round > latestCompletedSchedule.getReviewRound()) {

                LocalDate completedDate =
                        latestCompletedSchedule.getCompletedAt().toLocalDate();

                int plusDays = getReviewDayByRound(round)
                        - getReviewDayByRound(latestCompletedSchedule.getReviewRound());

                baseDate = completedDate;
                reviewDate = completedDate.plusDays(plusDays);

            } else {
                baseDate = getSolvedBaseDate(solvedProblem);
                reviewDate = baseDate.plusDays(REVIEW_DAYS[i]);
            }

            ReviewSchedule newSchedule = new ReviewSchedule(
                    solvedProblem,
                    round,
                    baseDate,
                    reviewDate
            );

            reviewScheduleRepository.save(newSchedule);
            createdCount++;
        }

        return createdCount;
    }

    @Transactional
    public int createReviewSchedulesForProblems(List<SolvedProblem> solvedProblems) {
        int createdProblemCount = 0;

        for (SolvedProblem solvedProblem : solvedProblems) {
            if (reviewScheduleRepository.existsBySolvedProblemId(solvedProblem.getId())) {
                continue;
            }

            LocalDate baseDate = getSolvedBaseDate(solvedProblem);
            createReviewSchedulesByBaseDate(solvedProblem, baseDate);

            createdProblemCount++;
        }

        return createdProblemCount;
    }

    private LocalDate getSolvedBaseDate(SolvedProblem solvedProblem) {
        if (solvedProblem.getSolvedDate() != null) {
            return solvedProblem.getSolvedDate();
        }

        return LocalDate.now();
    }

    private void createReviewSchedulesByBaseDate(SolvedProblem solvedProblem, LocalDate baseDate) {
        for (int i = 0; i < REVIEW_DAYS.length; i++) {
            int round = i + 1;

            boolean exists = reviewScheduleRepository.existsBySolvedProblemIdAndReviewRound(
                    solvedProblem.getId(),
                    round
            );

            if (exists) {
                continue;
            }

            ReviewSchedule reviewSchedule = new ReviewSchedule(
                    solvedProblem,
                    round,
                    baseDate,
                    baseDate.plusDays(REVIEW_DAYS[i])
            );

            reviewScheduleRepository.save(reviewSchedule);
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getTodayReviews(Long userId) {
        return reviewScheduleRepository
                .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        LocalDate.now(),
                        STATUS_PENDING
                );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getUpcomingReviews(Long userId, int days) {
        LocalDate today = LocalDate.now();

        LocalDate startDate = today.minusDays(OVERDUE_LOOKBACK_DAYS);
        LocalDate endDate = today.plusDays(days);

        return reviewScheduleRepository.findUpcomingReviewsByUser(
                userId,
                STATUS_PENDING,
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getCompletedReviews(Long userId) {
        return reviewScheduleRepository.findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
                userId,
                STATUS_COMPLETED
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getProblemReviewSchedules(Long solvedProblemId) {
        return reviewScheduleRepository.findBySolvedProblemIdOrderByReviewRoundAsc(solvedProblemId);
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
        int currentDay = getReviewDayByRound(currentRound);
        int nextDay = getReviewDayByRound(nextRound);

        return nextDay - currentDay;
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
    public void retryReview(Long reviewScheduleId) {
        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복습 일정을 찾을 수 없습니다."));

        reviewSchedule.retry();
    }

    @Transactional
    public int deletePendingReviewSchedulesBeforeDate(Long userId, LocalDate startDate) {
        List<ReviewSchedule> schedules =
                reviewScheduleRepository.findBySolvedProblemUserIdAndStatusAndReviewDateBefore(
                        userId,
                        STATUS_PENDING,
                        startDate
                );

        int deletedCount = schedules.size();
        reviewScheduleRepository.deleteAll(schedules);

        return deletedCount;
    }

    public record ReviewRebuildResult(
            int deletedCount,
            int createdProblemCount,
            int createdScheduleCount
    ) {
    }
}