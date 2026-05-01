package com.algotrail.backend.domain.statistics.dto;

import java.util.List;

public record CategoryDistributionResponse(
        long totalCount,
        List<CategoryDistributionItem> items
) {
    public record CategoryDistributionItem(
            String categoryName,
            long count,
            double percentage
    ) {
    }
}