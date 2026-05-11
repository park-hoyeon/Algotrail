package com.algotrail.backend.domain.tag.exception;

public class GeminiRateLimitException extends RuntimeException {

    public GeminiRateLimitException(String message) {
        super(message);
    }
}