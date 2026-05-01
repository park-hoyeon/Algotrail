package com.algotrail.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."),
    SOLVED_PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 풀이 기록입니다."),
    REVIEW_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 복습 일정입니다."),
    GITHUB_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 동기화 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}