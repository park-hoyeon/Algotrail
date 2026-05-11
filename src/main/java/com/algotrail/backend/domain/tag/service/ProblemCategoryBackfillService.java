package com.algotrail.backend.domain.tag.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.ProblemRepository;
import com.algotrail.backend.domain.tag.dto.TagResolveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemCategoryBackfillService {

    private final ProblemRepository problemRepository;
    private final ProblemTagResolver problemTagResolver;
    private final CategoryRepository categoryRepository;
    private final ProblemCategoryRepository problemCategoryRepository;

    @Transactional
    public int backfillAll() {
        int count = 0;

        for (Problem problem : problemRepository.findAll()) {
            if (problem.getProblemNumber() == null) {
                continue;
            }

            TagResolveResult result = problemTagResolver.resolve(
                    problem.getPlatform(),
                    problem.getProblemNumber(),
                    problem.getTitle(),
                    null,
                    null
            );

            Category category = categoryRepository.findByName(result.categoryName())
                    .orElse(null);

            if (category == null) {
                continue;
            }

            List<ProblemCategory> existingCategories =
                    problemCategoryRepository.findByProblem(problem);

            if (!existingCategories.isEmpty()) {
                problemCategoryRepository.deleteAll(existingCategories);
                problemCategoryRepository.flush();
            }

            problemCategoryRepository.save(new ProblemCategory(problem, category));

            count++;
        }

        return count;
    }
}