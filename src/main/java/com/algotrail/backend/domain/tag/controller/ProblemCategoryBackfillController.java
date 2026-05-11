package com.algotrail.backend.domain.tag.controller;

import com.algotrail.backend.domain.tag.service.ProblemCategoryBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class ProblemCategoryBackfillController {

    private final ProblemCategoryBackfillService problemCategoryBackfillService;

    @PostMapping("/backfill")
    public String backfill() {
        int count = problemCategoryBackfillService.backfillAll();
        return count + "개 문제 유형 재분류 완료";
    }
}