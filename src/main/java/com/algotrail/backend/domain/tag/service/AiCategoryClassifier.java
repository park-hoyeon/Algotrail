package com.algotrail.backend.domain.tag.service;

import com.algotrail.backend.domain.tag.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiCategoryClassifier {

    private final GeminiClient geminiClient;

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "구현",
            "BFS/DFS",
            "DP",
            "그리디",
            "스택/큐",
            "힙",
            "해시",
            "정렬",
            "이분탐색",
            "완전탐색",
            "백트래킹",
            "그래프",
            "최단경로",
            "문자열",
            "수학",
            "자료구조",
            "트리",
            "Union-Find",
            "미분류"
    );

    public String classify(
            String platform,
            String problemTitle,
            String language,
            String codeContent
    ) {
        String result = geminiClient.classifyAlgorithmType(
                platform,
                problemTitle,
                language,
                codeContent
        );

        String normalized = normalize(result);

        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            return "미분류";
        }

        return normalized;
    }

    private String normalize(String result) {
        if (result == null) {
            return "미분류";
        }

        return result
                .replace("`", "")
                .replace(".", "")
                .replace("\n", "")
                .trim();
    }
}