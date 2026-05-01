package com.algotrail.backend.global.config;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        saveIfNotExists("구현", "문제 조건을 코드로 정확히 구현하는 유형");
        saveIfNotExists("BFS/DFS", "그래프 탐색 유형");
        saveIfNotExists("자료구조", "배열, 리스트, 맵, 셋 등을 활용하는 유형");
        saveIfNotExists("DP", "동적 계획법 유형");
        saveIfNotExists("스택/큐", "스택과 큐를 활용하는 유형");
        saveIfNotExists("해시", "HashMap, HashSet 활용 유형");
        saveIfNotExists("정렬", "정렬 기준 설계 유형");
        saveIfNotExists("이분탐색", "탐색 범위를 절반씩 줄이는 유형");
        saveIfNotExists("백트래킹", "가능한 경우를 탐색하고 되돌리는 유형");
        saveIfNotExists("최단경로", "다익스트라, 플로이드 워셜 등");
        saveIfNotExists("힙", "우선순위 큐 활용 유형");
        saveIfNotExists("그래프", "노드와 간선 기반 문제");
        saveIfNotExists("그리디", "현재 최적 선택을 반복하는 유형");
        saveIfNotExists("투 포인터", "두 개의 포인터를 활용하는 유형");
        saveIfNotExists("완전탐색", "모든 경우의 수를 확인하는 유형");
        saveIfNotExists("문자열", "문자열 처리 유형");
        saveIfNotExists("수학", "수식, 약수, 소수, 조합 등");
        saveIfNotExists("시뮬레이션", "상황을 순서대로 재현하는 유형");
        saveIfNotExists("트리", "트리 구조 활용 유형");
        saveIfNotExists("Union-Find", "분리 집합 유형");
    }

    private void saveIfNotExists(String name, String description) {
        if (!categoryRepository.existsByName(name)) {
            categoryRepository.save(new Category(name, description));
        }
    }
}