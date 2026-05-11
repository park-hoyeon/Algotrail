package com.algotrail.backend.domain.tag.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryMappingService {

    public String mapSolvedAcTagsToCategory(List<String> tagKeys) {
        if (tagKeys == null || tagKeys.isEmpty()) {
            return "구현";
        }

        if (containsAny(tagKeys, "bfs", "dfs", "graph_traversal")) return "BFS/DFS";
        if (containsAny(tagKeys, "dijkstra", "floyd_warshall", "shortest_path")) return "최단경로";
        if (containsAny(tagKeys, "dp")) return "DP";
        if (containsAny(tagKeys, "greedy")) return "그리디";
        if (containsAny(tagKeys, "data_structures")) return "자료구조";
        if (containsAny(tagKeys, "stack", "queue", "deque")) return "스택/큐";
        if (containsAny(tagKeys, "hash_set", "hash_map")) return "해시";
        if (containsAny(tagKeys, "sorting")) return "정렬";
        if (containsAny(tagKeys, "binary_search", "parametric_search")) return "이분탐색";
        if (containsAny(tagKeys, "backtracking")) return "백트래킹";
        if (containsAny(tagKeys, "graphs")) return "그래프";
        if (containsAny(tagKeys, "bruteforcing")) return "완전탐색";
        if (containsAny(tagKeys, "string")) return "문자열";
        if (containsAny(tagKeys, "math", "number_theory", "combinatorics")) return "수학";
        if (containsAny(tagKeys, "simulation", "implementation")) return "구현";
        if (containsAny(tagKeys, "trees")) return "트리";
        if (containsAny(tagKeys, "disjoint_set")) return "Union-Find";

        return "구현";
    }

    private boolean containsAny(List<String> tagKeys, String... targets) {
        for (String target : targets) {
            if (tagKeys.contains(target)) {
                return true;
            }
        }
        return false;
    }
}