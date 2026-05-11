package com.algotrail.backend.domain.tag.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final WebClient webClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    public String classifyAlgorithmType(
            String platform,
            String problemTitle,
            String language,
            String codeContent
    ) {
        try {
            String prompt = buildPrompt(platform, problemTitle, language, codeContent);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            Map response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractText(response);

        } catch (Exception e) {
            System.out.println("[Gemini 분류 실패] " + problemTitle + " / " + e.getMessage());
            return "미분류";
        }
    }

    private String buildPrompt(
            String platform,
            String problemTitle,
            String language,
            String codeContent
    ) {
        String safeCode = codeContent == null ? "" : codeContent;

        if (safeCode.length() > 6000) {
            safeCode = safeCode.substring(0, 6000);
        }

        return """
                너는 알고리즘 문제 풀이 코드를 보고 대표 유형을 하나만 분류하는 분류기야.

                반드시 아래 목록 중 하나만 출력해.
                다른 설명, 문장, 마침표, JSON 없이 카테고리명 하나만 출력해.

                사용 가능한 카테고리:
                구현
                BFS/DFS
                DP
                그리디
                스택/큐
                힙
                해시
                정렬
                이분탐색
                완전탐색
                백트래킹
                그래프
                최단경로
                문자열
                수학
                자료구조
                트리
                Union-Find
                미분류

                판단 기준:
                - deque, queue, stack 중심이면 스택/큐
                - bfs, dfs, visited, graph traversal이면 BFS/DFS
                - dp 배열, 점화식, memoization이면 DP
                - heapq, priority queue면 힙
                - dict, HashMap, Counter 중심이면 해시
                - sort 중심이면 정렬
                - binary search, left/right/mid면 이분탐색
                - 모든 경우를 생성하거나 순열/조합 탐색이면 완전탐색
                - 재귀로 후보를 되돌리며 탐색하면 백트래킹
                - 다익스트라, 플로이드, 최단 거리면 최단경로
                - union find, parent, find/union이면 Union-Find
                - 문자열 파싱/분할/치환 중심이면 문자열
                - 단순 조건/반복/시뮬레이션이면 구현

                플랫폼: %s
                문제 제목: %s
                언어: %s

                코드:
                ```%s
                %s
                ```
                """.formatted(platform, problemTitle, language, language, safeCode);
    }

    private String extractText(Map response) {
        if (response == null) {
            return "미분류";
        }

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            return "미분류";
        }

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");

        if (content == null) {
            return "미분류";
        }

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        if (parts == null || parts.isEmpty()) {
            return "미분류";
        }

        return String.valueOf(parts.get(0).get("text")).trim();
    }
}