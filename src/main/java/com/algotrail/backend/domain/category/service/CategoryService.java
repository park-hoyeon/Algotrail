package com.algotrail.backend.domain.category.service;

import com.algotrail.backend.domain.category.dto.CategoryBackfillResponse;
import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProblemRepository problemRepository;
    private final ProblemCategoryRepository problemCategoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryBackfillResponse backfillProblemCategories() {
        List<Problem> problems = problemRepository.findAll();

        int updatedCount = 0;
        int skippedCount = 0;

        for (Problem problem : problems) {
            List<ProblemCategory> existingCategories =
                    problemCategoryRepository.findByProblem(problem);

            if (!existingCategories.isEmpty()) {
                skippedCount++;
                continue;
            }

            String categoryName = inferCategoryName(problem.getTitle());

            Category category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() ->
                            new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryName)
                    );

            problemCategoryRepository.save(new ProblemCategory(problem, category));
            updatedCount++;
        }

        return new CategoryBackfillResponse(
                updatedCount,
                skippedCount,
                updatedCount + "개 문제에 카테고리를 자동 지정했습니다. "
                        + skippedCount + "개 문제는 이미 카테고리가 있어 건너뛰었습니다."
        );
    }

    private String inferCategoryName(String problemTitle) {
        String title = problemTitle.toLowerCase();

        if (title.contains("dfs")
                || title.contains("bfs")
                || title.contains("네트워크")
                || title.contains("타겟 넘버")
                || title.contains("게임 맵 최단거리")) {
            return "BFS/DFS";
        }

        if (title.contains("배달")
                || title.contains("최단")
                || title.contains("다익스트라")
                || title.contains("순위")) {
            return "최단경로";
        }

        if (title.contains("해시")
                || title.contains("완주하지 못한 선수")
                || title.contains("전화번호")
                || title.contains("위장")
                || title.contains("베스트앨범")) {
            return "해시";
        }

        if (title.contains("스택")
                || title.contains("큐")
                || title.contains("기능개발")
                || title.contains("올바른 괄호")
                || title.contains("프로세스")
                || title.contains("다리를 지나는 트럭")) {
            return "스택/큐";
        }

        if (title.contains("정렬")
                || title.contains("k번째수")
                || title.contains("가장 큰 수")
                || title.contains("h-index")) {
            return "정렬";
        }

        if (title.contains("dp")
                || title.contains("타일")
                || title.contains("정수 삼각형")
                || title.contains("등굣길")
                || title.contains("n으로 표현")) {
            return "DP";
        }

        if (title.contains("그리디")
                || title.contains("체육복")
                || title.contains("구명보트")
                || title.contains("조이스틱")
                || title.contains("큰 수 만들기")) {
            return "그리디";
        }

        if (title.contains("카펫")
                || title.contains("모의고사")
                || title.contains("소수 찾기")
                || title.contains("피로도")
                || title.contains("전력망")) {
            return "완전탐색";
        }

        if (title.contains("이분")
                || title.contains("입국심사")
                || title.contains("징검다리")) {
            return "이분탐색";
        }

        if (title.contains("힙")
                || title.contains("더 맵게")
                || title.contains("디스크 컨트롤러")
                || title.contains("이중우선순위큐")) {
            return "힙";
        }

        if (title.contains("문자열")
                || title.contains("문자")
                || title.contains("이상한 문자")
                || title.contains("문자열 압축")) {
            return "문자열";
        }

        if (title.contains("약수")
                || title.contains("소수")
                || title.contains("최대공약수")
                || title.contains("최소공배수")) {
            return "수학";
        }

        return "구현";
    }

    public record CategoryResponse(
            Long categoryId,
            String name,
            String description
    ) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getDescription()
            );
        }
    }
}