package com.algotrail.backend.domain.tag.dto;

import com.algotrail.backend.domain.tag.entity.CategorySource;

public record TagResolveResult(
        String categoryName,
        CategorySource source
) {
}