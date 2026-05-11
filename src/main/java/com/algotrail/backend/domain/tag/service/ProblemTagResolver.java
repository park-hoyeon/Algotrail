package com.algotrail.backend.domain.tag.service;

import com.algotrail.backend.domain.tag.client.SolvedAcClient;
import com.algotrail.backend.domain.tag.dto.TagResolveResult;
import com.algotrail.backend.domain.tag.entity.CategorySource;
import com.algotrail.backend.domain.tag.entity.ProblemTagCache;
import com.algotrail.backend.domain.tag.repository.ProblemTagCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTagResolver {

    private final ProblemTagCacheRepository problemTagCacheRepository;
    private final SolvedAcClient solvedAcClient;
    private final CategoryMappingService categoryMappingService;
    private final AiCategoryClassifier aiCategoryClassifier;

    @Transactional
    public TagResolveResult resolve(
            String platform,
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent
    ) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedTitle = normalizeProblemTitle(normalizedPlatform, problemTitle);

        return problemTagCacheRepository
                .findByPlatformAndProblemNumber(normalizedPlatform, problemNumber)
                .map(cache -> new TagResolveResult(cache.getCategoryName(), cache.getSource()))
                .orElseGet(() -> resolveAndCache(
                        normalizedPlatform,
                        problemNumber,
                        normalizedTitle,
                        language,
                        codeContent
                ));
    }

    private TagResolveResult resolveAndCache(
            String platform,
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent
    ) {
        TagResolveResult result;

        if ("BAEKJOON".equals(platform)) {
            result = resolveBaekjoon(problemNumber);
        } else if ("PROGRAMMERS".equals(platform)) {
            result = resolveProgrammers(problemNumber, problemTitle, language, codeContent);
        } else {
            result = new TagResolveResult("미분류", CategorySource.UNKNOWN);
        }

        String categoryName = normalizeCategoryName(result.categoryName());

        if (shouldCache(categoryName, result.source())) {
            problemTagCacheRepository.save(new ProblemTagCache(
                    platform,
                    problemNumber,
                    problemTitle,
                    categoryName,
                    result.source()
            ));
        } else {
            System.out.println("[Tag Cache 저장 안 함] "
                    + platform + " " + problemNumber
                    + " / category=" + categoryName
                    + " / source=" + result.source());
        }

        return new TagResolveResult(categoryName, result.source());
    }

    private boolean shouldCache(String categoryName, CategorySource source) {
        if (categoryName == null || categoryName.isBlank()) {
            return false;
        }

        if ("미분류".equals(categoryName)) {
            return false;
        }

        return source != CategorySource.UNKNOWN;
    }

    private String normalizePlatform(String platform) {
        if (platform == null) return "UNKNOWN";

        if (platform.equals("백준")
                || platform.equalsIgnoreCase("BAEKJOON")
                || platform.equalsIgnoreCase("BOJ")) {
            return "BAEKJOON";
        }

        if (platform.equals("프로그래머스")
                || platform.equalsIgnoreCase("PROGRAMMERS")) {
            return "PROGRAMMERS";
        }

        if (platform.equalsIgnoreCase("LEETCODE")) {
            return "LEETCODE";
        }

        return platform;
    }

    private String normalizeProblemTitle(String platform, String title) {
        if (title == null) {
            return "";
        }

        String normalized = title
                .replace('\u00A0', ' ')
                .replace('\u2005', ' ')
                .replace('\u200B', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        if ("PROGRAMMERS".equals(platform)) {
            normalized = normalized
                    .replaceAll("^\\d+\\.\\s*", "")
                    .trim();
        }

        return normalized;
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return "미분류";
        }

        return categoryName.trim();
    }

    private TagResolveResult resolveBaekjoon(Long problemNumber) {
        List<String> tagKeys = solvedAcClient.getTagKeys(problemNumber);

        if (tagKeys.isEmpty()) {
            return new TagResolveResult("미분류", CategorySource.UNKNOWN);
        }

        String categoryName = categoryMappingService.mapSolvedAcTagsToCategory(tagKeys);

        if (categoryName == null || categoryName.isBlank()) {
            return new TagResolveResult("미분류", CategorySource.UNKNOWN);
        }

        return new TagResolveResult(categoryName, CategorySource.SOLVED_AC);
    }

    private TagResolveResult resolveProgrammers(
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent
    ) {
        System.out.println("[Programmers Rule 기반 분류 요청]");
        System.out.println("problemNumber = " + problemNumber);
        System.out.println("title = " + problemTitle);
        System.out.println("language = " + language);
        System.out.println("code length = " + (codeContent == null ? 0 : codeContent.length()));

        String categoryName = aiCategoryClassifier.classify(
                "PROGRAMMERS",
                problemNumber,
                problemTitle,
                language,
                codeContent
        );

        categoryName = normalizeCategoryName(categoryName);

        System.out.println("[Programmers Rule 기반 결과] " + problemTitle + " -> " + categoryName);

        if ("미분류".equals(categoryName)) {
            return new TagResolveResult("미분류", CategorySource.UNKNOWN);
        }

        return new TagResolveResult(categoryName, CategorySource.AI_INFERRED);
    }

    @Transactional
    public void saveUserCorrectedCategory(
            String platform,
            Long problemNumber,
            String problemTitle,
            String categoryName
    ) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedTitle = normalizeProblemTitle(normalizedPlatform, problemTitle);
        String normalizedCategoryName = normalizeCategoryName(categoryName);

        ProblemTagCache cache = problemTagCacheRepository
                .findByPlatformAndProblemNumber(normalizedPlatform, problemNumber)
                .orElseGet(() -> problemTagCacheRepository.save(new ProblemTagCache(
                        normalizedPlatform,
                        problemNumber,
                        normalizedTitle,
                        normalizedCategoryName,
                        CategorySource.USER_CORRECTED
                )));

        cache.updateCategory(normalizedCategoryName, CategorySource.USER_CORRECTED);
    }
}