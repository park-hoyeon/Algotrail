package com.algotrail.backend.domain.tag.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.tag.dto.TagResolveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiProblemClassificationService {

    private static final String DEFAULT_CATEGORY_NAME = "미분류";

    private final WebClient webClient;
    private final ProblemCategoryRepository problemCategoryRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final CategoryRepository categoryRepository;
    private final ProblemTagResolver problemTagResolver;

    @Transactional
    public void classifyPendingProblems(int batchSize) {
        List<ProblemCategory> pendingList =
                problemCategoryRepository.findByCategoryName(
                        DEFAULT_CATEGORY_NAME,
                        PageRequest.of(0, batchSize)
                );

        if (pendingList.isEmpty()) {
            System.out.println("[AI Scheduler] 분류할 미분류 문제가 없습니다.");
            return;
        }

        for (ProblemCategory problemCategory : pendingList) {
            boolean success = classifyOne(problemCategory);

            if (!success) {
                return;
            }
        }
    }

    private boolean classifyOne(ProblemCategory problemCategory) {
        Problem problem = problemCategory.getProblem();

        Optional<SolvedProblem> solvedProblemOptional =
                solvedProblemRepository.findFirstByProblemOrderBySolvedDateDesc(problem);

        if (solvedProblemOptional.isEmpty()) {
            System.out.println("[AI 분류 스킵] 풀이 기록 없음: " + problem.getTitle());
            return true;
        }

        SolvedProblem solvedProblem = solvedProblemOptional.get();

        String codeContent = fetchCodeContentFromGithubUrl(solvedProblem.getGithubUrl());

        TagResolveResult result;

        try {
            result = problemTagResolver.resolve(
                    problem.getPlatform(),
                    problem.getProblemNumber(),
                    problem.getTitle(),
                    solvedProblem.getLanguage(),
                    codeContent
            );
        } catch (com.algotrail.backend.domain.tag.exception.GeminiRateLimitException e) {
            System.out.println("[AI 분류 보류] "
                    + problem.getProblemNumber()
                    + ". "
                    + problem.getTitle()
                    + " / "
                    + e.getMessage());

            return false;
        }

        String categoryName = normalizeCategoryName(result.categoryName());

        if (DEFAULT_CATEGORY_NAME.equals(categoryName)) {
            System.out.println("[AI 분류 결과 미분류 유지] " + problem.getTitle());
            return true;
        }

        Category category = categoryRepository.findByName(categoryName)
                .orElse(null);

        if (category == null) {
            System.out.println("[AI 분류 실패] DB에 없는 카테고리: " + categoryName);
            return true;
        }

        List<ProblemCategory> existingCategories =
                problemCategoryRepository.findByProblem(problem);

        if (!existingCategories.isEmpty()) {
            problemCategoryRepository.deleteAll(existingCategories);
            problemCategoryRepository.flush();
        }

        problemCategoryRepository.save(new ProblemCategory(problem, category));

        System.out.println("[AI 백그라운드 분류 완료] "
                + problem.getTitle() + " -> " + categoryName);

        return true;
    }

    private String fetchCodeContentFromGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return "";
        }

        try {
            String rawUrl = convertGithubBlobUrlToRawUrl(githubUrl);

            System.out.println("[AI 분류용 GitHub URL]");
            System.out.println("htmlUrl = " + githubUrl);
            System.out.println("rawUrl = " + rawUrl);

            String content = webClient.get()
                    .uri(java.net.URI.create(rawUrl))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (content == null || content.isBlank()) {
                return "";
            }

            return content.length() > 3000
                    ? content.substring(0, 3000)
                    : content;

        } catch (Exception e) {
            System.out.println("[AI 분류용 코드 조회 실패] " + e.getMessage());
            return "";
        }
    }

    private String convertGithubBlobUrlToRawUrl(String githubUrl) {
        return githubUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replace("/blob/", "/");
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return DEFAULT_CATEGORY_NAME;
        }

        return categoryName.trim();
    }
}