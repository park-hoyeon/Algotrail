package com.algotrail.backend.domain.tag.service;

import org.springframework.stereotype.Service;

@Service
public class AiCategoryClassifier {

    public String classify(
            String platform,
            Long problemNumber,
            String problemTitle,
            String language,
            String codeContent
    ) {
        System.out.println("[Rule 기반 분류 시작]");
        System.out.println("platform = " + platform);
        System.out.println("problemNumber = " + problemNumber);
        System.out.println("title = " + problemTitle);
        System.out.println("language = " + language);
        System.out.println("codeLength = " + (codeContent == null ? 0 : codeContent.length()));

        String result = classifyByLocalRule(problemTitle, codeContent);

        System.out.println("[Rule 기반 분류 결과] " + problemTitle + " -> " + result);

        return result;
    }

    private String classifyByLocalRule(String problemTitle, String codeContent) {
        String title = normalize(problemTitle);
        String code = normalize(codeContent).toLowerCase();

        // 1. 최단경로
        if (containsAny(code, "dijkstra", "dist", "distance", "shortest", "floyd", "floyd-warshall")
                || containsAny(title, "최단", "경로", "배달", "합승", "택시", "순위")) {
            return "최단경로";
        }

        // 2. Union-Find
        if ((code.contains("find") && code.contains("union"))
                || containsAny(code, "parent", "parents", "root")
                || containsAny(title, "섬 연결", "네트워크", "연결", "집합")) {
            return "Union-Find";
        }

        // 3. BFS/DFS
        if (containsAny(code, "deque", "queue", "bfs", "dfs", "visited")
                || (code.contains("dx") && code.contains("dy"))
                || containsAny(title, "게임 맵", "미로", "거리두기", "단어 변환", "타겟 넘버", "네트워크")) {
            return "BFS/DFS";
        }

        // 4. 힙
        if (containsAny(code, "heapq", "heappush", "heappop", "priorityqueue", "priority_queue")
                || containsAny(title, "이중우선순위큐", "디스크 컨트롤러", "더 맵게", "야근 지수")) {
            return "힙";
        }

        // 5. DP
        if (containsAny(code, "dp", "memo", "cache")
                || containsAny(title, "정수 삼각형", "등굣길", "도둑질", "n으로 표현", "타일", "피보나치")) {
            return "DP";
        }

        // 6. 해시
        if (containsAny(code, "counter", "hashmap", "dict", "map.get", "dictionary")
                || containsAny(title, "완주하지 못한 선수", "폰켓몬", "위장", "베스트앨범", "전화번호 목록")) {
            return "해시";
        }

        // 7. 정렬
        if (containsAny(code, "sort", "sorted", "comparator", "lambda")
                || containsAny(title, "가장 큰 수", "h-index", "k번째수", "정렬")) {
            return "정렬";
        }

        // 8. 이분탐색
        if ((code.contains("left") && code.contains("right") && code.contains("mid"))
                || containsAny(code, "binarysearch", "lower_bound", "upper_bound")
                || containsAny(title, "입국심사", "징검다리", "예산", "이분", "탐색")) {
            return "이분탐색";
        }

        // 9. 완전탐색
        if (containsAny(code, "permutation", "permutations", "combination", "combinations", "product(")
                || containsAny(title, "모의고사", "소수 찾기", "카펫", "피로도", "전력망", "모음사전")) {
            return "완전탐색";
        }

        // 10. 백트래킹
        if (containsAny(code, "backtrack", "backtracking")
                || (code.contains("recursive") && code.contains("pop"))
                || containsAny(title, "양궁대회", "퍼즐", "n-queen", "순열")) {
            return "백트래킹";
        }

        // 11. 스택/큐
        if (containsAny(code, "stack", ".pop()", ".append(", "popleft")
                || containsAny(title, "같은 숫자는 싫어", "기능개발", "프로세스", "다리를 지나는 트럭", "올바른 괄호", "주식가격")) {
            return "스택/큐";
        }

        // 12. 그래프
        if (containsAny(code, "graph", "edges", "adj", "adjacency")
                || containsAny(title, "그래프", "가장 먼 노드", "방의 개수", "순위")) {
            return "그래프";
        }

        // 13. 트리
        if (containsAny(code, "tree", "node", "preorder", "postorder", "inorder")
                || containsAny(title, "트리", "길 찾기 게임", "양과 늑대")) {
            return "트리";
        }

        // 14. 문자열
        if (containsAny(code, "split", "replace", "join", "substring", "charat", "startswith", "endswith")
                || containsAny(title, "문자열", "문자", "압축", "괄호 변환", "신규 아이디", "튜플", "파일명")) {
            return "문자열";
        }

        // 15. 수학
        if (containsAny(code, "gcd", "lcm", "sqrt", "pow", "math", "%")
                || containsAny(title, "약수", "소수", "나머지", "계산", "수열", "숫자", "최대공약수", "최소공배수", "행렬")) {
            return "수학";
        }

        // 16. 자료구조
        if (containsAny(code, "set", "list", "arraylist", "hashset", "linkedlist")
                || containsAny(title, "자료구조", "큐", "스택")) {
            return "자료구조";
        }

        if (!code.isBlank()) {
            return "구현";
        }

        return "미분류";
    }

    private boolean containsAny(String target, String... keywords) {
        if (target == null || target.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (target.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace('\u00A0', ' ')
                .replace('\u2005', ' ')
                .replace('\u200B', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}