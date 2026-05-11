package com.algotrail.backend.domain.tag.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"platform", "problemNumber"})
        }
)
public class ProblemTagCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private Long problemNumber;

    @Column(nullable = false)
    private String problemTitle;

    @Column(nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorySource source;

    public ProblemTagCache(
            String platform,
            Long problemNumber,
            String problemTitle,
            String categoryName,
            CategorySource source
    ) {
        this.platform = platform;
        this.problemNumber = problemNumber;
        this.problemTitle = problemTitle;
        this.categoryName = categoryName;
        this.source = source;
    }

    public void updateCategory(String categoryName, CategorySource source) {
        this.categoryName = categoryName;
        this.source = source;
    }
}