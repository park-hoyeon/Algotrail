package com.algotrail.backend.domain.statistics.service;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import com.algotrail.backend.domain.statistics.dto.ActivityHeatmapResponse;
import com.algotrail.backend.domain.statistics.dto.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.algotrail.backend.domain.statistics.dto.CategoryDistributionResponse;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final ProblemCategoryRepository problemCategoryRepository;

    public StatisticsResponse getStatistics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        long totalSolvedCount = solvedProblemRepository.countByUserId(userId);
        long todaySolvedCount = solvedProblemRepository.countByUserIdAndSolvedDate(userId, today);
        long reviewCompletedCount = reviewScheduleRepository.countBySolvedProblemUserIdAndStatus(
                userId,
                "COMPLETED"
        );

        List<SolvedProblem> allSolvedProblems = solvedProblemRepository.findByUserId(userId);

        if (allSolvedProblems == null) {
            allSolvedProblems = List.of();
        }

        List<StatisticsResponse.LevelStat> levelStats = allSolvedProblems.stream()
                .filter(solvedProblem -> solvedProblem.getProblem() != null)
                .collect(Collectors.groupingBy(
                        solvedProblem -> {
                            String level = solvedProblem.getProblem().getLevel();

                            if (level == null || level.isBlank()) {
                                return "Lv.미정";
                            }

                            return level;
                        },
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new StatisticsResponse.LevelStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(stat -> {
                    try {
                        return Integer.parseInt(stat.level().replace("Lv.", ""));
                    } catch (Exception e) {
                        return 999;
                    }
                }))
                .toList();

        List<SolvedProblem> weeklySolvedProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateBetween(userId, startDate, today);

        Map<LocalDate, Long> weeklyCountMap = weeklySolvedProblems.stream()
                .collect(Collectors.groupingBy(
                        SolvedProblem::getSolvedDate,
                        Collectors.counting()
                ));

        List<StatisticsResponse.DailyStat> weeklyStats = startDate.datesUntil(today.plusDays(1))
                .map(date -> new StatisticsResponse.DailyStat(
                        date.toString(),
                        weeklyCountMap.getOrDefault(date, 0L)
                ))
                .toList();

        return new StatisticsResponse(
                totalSolvedCount,
                todaySolvedCount,
                reviewCompletedCount,
                levelStats,
                weeklyStats
        );
    }

    public ActivityHeatmapResponse getActivityHeatmap(Long userId, int weeks) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks).plusDays(1);

        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateBetween(
                        userId,
                        startDate,
                        endDate
                );

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<ReviewSchedule> completedReviews =
                reviewScheduleRepository.findBySolvedProblemUserIdAndStatusAndCompletedAtBetween(
                        userId,
                        "COMPLETED",
                        startDateTime,
                        endDateTime
                );

        Map<LocalDate, Long> solvedCountMap = solvedProblems.stream()
                .collect(Collectors.groupingBy(
                        SolvedProblem::getSolvedDate,
                        Collectors.counting()
                ));

        Map<LocalDate, Long> reviewCountMap = new HashMap<>();

        for (ReviewSchedule review : completedReviews) {
            if (review.getCompletedAt() == null) {
                continue;
            }

            LocalDate completedDate = review.getCompletedAt().toLocalDate();
            reviewCountMap.put(
                    completedDate,
                    reviewCountMap.getOrDefault(completedDate, 0L) + 1
            );
        }

        List<ActivityHeatmapResponse.ActivityHeatmapItem> items = new ArrayList<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            long solvedCount = solvedCountMap.getOrDefault(currentDate, 0L);
            long reviewCompletedCount = reviewCountMap.getOrDefault(currentDate, 0L);
            long totalCount = solvedCount + reviewCompletedCount;

            int level = calculateActivityLevel(totalCount);

            items.add(new ActivityHeatmapResponse.ActivityHeatmapItem(
                    currentDate,
                    solvedCount,
                    reviewCompletedCount,
                    totalCount,
                    level
            ));

            currentDate = currentDate.plusDays(1);
        }

        return new ActivityHeatmapResponse(weeks, items);
    }

    private int calculateActivityLevel(long totalCount) {
        if (totalCount == 0) return 0;
        if (totalCount <= 2) return 1;
        if (totalCount <= 4) return 2;
        return 3;
    }

    public CategoryDistributionResponse getCategoryDistribution(Long userId) {
        List<Object[]> results = problemCategoryRepository.countSolvedByCategory(userId);

        long totalCount = results.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        List<CategoryDistributionResponse.CategoryDistributionItem> items =
                results.stream()
                        .map(row -> {
                            String categoryName = (String) row[0];
                            Long count = (Long) row[1];

                            double percentage = totalCount == 0
                                    ? 0.0
                                    : Math.round((count * 1000.0 / totalCount)) / 10.0;

                            return new CategoryDistributionResponse.CategoryDistributionItem(
                                    categoryName,
                                    count,
                                    percentage
                            );
                        })
                        .sorted((a, b) -> Long.compare(b.count(), a.count()))
                        .toList();

        return new CategoryDistributionResponse(totalCount, items);
    }
}