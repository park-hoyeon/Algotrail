package com.algotrail.backend.domain.tag.client;

import com.algotrail.backend.domain.tag.exception.GeminiRateLimitException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GeminiClient {

    private final WebClient geminiWebClient = WebClient.builder().build();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String model;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "구현", "BFS/DFS", "DP", "그리디", "스택/큐",
            "힙", "해시", "정렬", "이분탐색", "완전탐색",
            "백트래킹", "그래프", "최단경로", "문자열",
            "수학", "자료구조", "트리", "Union-Find", "미분류"
    );

    @PostConstruct
    public void checkGeminiConfig() {
        System.out.println("[Gemini 설정 확인] model = " + model);
        System.out.println("[Gemini 설정 확인] apiKey = " + maskApiKey(apiKey));
    }

    public String classifyAlgorithmType(
            String platform,
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent
    ) {
        validateApiKey();

        String safeTitle = cleanText(problemTitle);
        String safeCode = cleanText(codeContent);
        String codeFeatures = extractCodeFeatures(safeCode);

        try {
            String prompt = buildPrompt(
                    platform,
                    problemNumber,
                    safeTitle,
                    language,
                    safeCode,
                    codeFeatures
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", prompt))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0,
                            "topP", 0.1,
                            "maxOutputTokens", 20
                    )
            );

            Map response = geminiWebClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String result = normalizeResult(extractText(response));

            if (!ALLOWED_TYPES.contains(result)) {
                System.out.println("[Gemini 이상 응답] " + result);
                return "미분류";
            }

            System.out.println("[Gemini 분류 성공] " + safeTitle + " -> " + result);
            return result;

        } catch (WebClientResponseException.TooManyRequests e) {
            System.out.println("[Gemini 429 발생] " + safeTitle);
            throw new GeminiRateLimitException("Gemini 요청 한도 초과로 다음 스케줄에서 재시도합니다.");

        } catch (Exception e) {
            System.out.println("[Gemini 분류 실패] " + safeTitle + " / " + e.getMessage());
            return "미분류";
        }
    }

    private String buildPrompt(
            String platform,
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent,
            String codeFeatures
    ) {
        String safeCode = codeContent == null ? "" : codeContent;

        if (safeCode.length() > 3000) {
            safeCode = safeCode.substring(0, 3000);
        }

        return """
                너는 알고리즘 문제 풀이 코드를 보고 대표 알고리즘 유형을 하나만 분류하는 분류기다.

                반드시 아래 카테고리 중 정확히 하나만 출력해.
                설명 금지.
                JSON 금지.
                문장 금지.
                카테고리명 하나만 출력.

                카테고리:
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

                플랫폼: %s
                문제 번호: %s
                문제 제목: %s
                언어: %s

                코드 특징:
                %s

                코드:
                ```%s
                %s
                ```
                """.formatted(
                platform,
                problemNumber,
                problemTitle,
                language,
                codeFeatures,
                language,
                safeCode
        );
    }

    private String extractCodeFeatures(String code) {
        if (code == null || code.isBlank()) {
            return "- 코드 없음";
        }

        String lower = code.toLowerCase();
        StringBuilder sb = new StringBuilder();

        if (lower.contains("deque") || lower.contains("queue")) sb.append("- 큐/deque 사용\n");
        if (lower.contains("visited")) sb.append("- visited 사용\n");
        if (lower.contains("dx") || lower.contains("dy")) sb.append("- 방향 배열 사용\n");
        if (lower.contains("dfs") || lower.contains("bfs")) sb.append("- dfs/bfs 함수명 사용\n");
        if (lower.contains("dp")) sb.append("- dp 키워드 사용\n");
        if (lower.contains("heapq") || lower.contains("priorityqueue")) sb.append("- 힙/우선순위 큐 사용\n");
        if (lower.contains("dict") || lower.contains("counter") || lower.contains("hashmap")) sb.append("- 해시 자료구조 사용\n");
        if (lower.contains("sort") || lower.contains("sorted")) sb.append("- 정렬 사용\n");
        if (lower.contains("left") && lower.contains("right") && lower.contains("mid")) sb.append("- 이분탐색 구조 사용\n");
        if (lower.contains("permutation") || lower.contains("combination")) sb.append("- 순열/조합 사용\n");
        if (lower.contains("gcd") || lower.contains("lcm")) sb.append("- gcd/lcm 수학 함수 사용\n");
        if (lower.contains("split") || lower.contains("replace") || lower.contains("join")) sb.append("- 문자열 처리 함수 사용\n");
        if (lower.contains("find") && lower.contains("union")) sb.append("- union-find 구조 사용\n");

        if (sb.isEmpty()) {
            sb.append("- 특별한 라이브러리 없이 조건문/반복문 중심");
        }

        return sb.toString();
    }

    private String extractText(Map response) {
        if (response == null) return "미분류";

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) return "미분류";

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");

        if (content == null) return "미분류";

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        if (parts == null || parts.isEmpty()) return "미분류";

        Object text = parts.get(0).get("text");

        if (text == null) return "미분류";

        return String.valueOf(text);
    }

    private String normalizeResult(String text) {
        String cleaned = cleanText(text)
                .replace("`", "")
                .replace(".", "")
                .replace(":", "")
                .replace("카테고리", "")
                .trim();

        if (cleaned.contains("BFS") || cleaned.contains("DFS")) return "BFS/DFS";
        if (cleaned.contains("Union")) return "Union-Find";

        return cleaned;
    }

    private String cleanText(String text) {
        if (text == null) return "";

        return text
                .replace('\u00A0', ' ')
                .replace('\u2005', ' ')
                .replace('\u200B', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 비어 있습니다.");
        }

        if (apiKey.contains("${")) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 치환되지 않았습니다.");
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 10) {
            return "INVALID_KEY";
        }

        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}