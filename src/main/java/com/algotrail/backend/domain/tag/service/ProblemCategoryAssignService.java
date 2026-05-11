package com.algotrail.backend.domain.tag.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.tag.dto.TagResolveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemCategoryAssignService {

    private static final String DEFAULT_CATEGORY_NAME = "미분류";

    private final ProblemCodeFetchService problemCodeFetchService;
    private final ProblemTagResolver problemTagResolver;
    private final CategoryRepository categoryRepository;
    private final ProblemCategoryRepository problemCategoryRepository;

    @Transactional
    public void classifyAndAssign(Problem problem, SolvedProblem solvedProblem) {
        if (problem == null || solvedProblem == null) {
            return;
        }

        String codeContent = problemCodeFetchService.fetchCodeContentFromGithubUrl(
                solvedProblem.getGithubUrl()
        );

        TagResolveResult result = problemTagResolver.resolve(
                problem.getPlatform(),
                problem.getProblemNumber(),
                problem.getTitle(),
                solvedProblem.getLanguage(),
                codeContent
        );

        String categoryName = normalizeCategoryName(result.categoryName());

        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> categoryRepository.findByName(DEFAULT_CATEGORY_NAME)
                        .orElse(null));

        if (category == null) {
            System.out.println("[초기 분류 실패] DB에 카테고리 없음: " + categoryName);
            return;
        }

        List<ProblemCategory> existingCategories =
                problemCategoryRepository.findByProblem(problem);

        if (!existingCategories.isEmpty()) {
            problemCategoryRepository.deleteAll(existingCategories);
            problemCategoryRepository.flush();
        }

        problemCategoryRepository.save(new ProblemCategory(problem, category));

        System.out.println("[초기 문제 유형 저장 완료] "
                + problem.getTitle()
                + " -> "
                + category.getName());
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return DEFAULT_CATEGORY_NAME;
        }

        return categoryName.trim();
    }
}