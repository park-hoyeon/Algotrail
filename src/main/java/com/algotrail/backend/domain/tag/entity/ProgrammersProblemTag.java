package com.algotrail.backend.domain.tag.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"problemTitle"})
})
public class ProgrammersProblemTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String problemTitle;

    @Column(nullable = false)
    private String categoryName;

    public ProgrammersProblemTag(String problemTitle, String categoryName) {
        this.problemTitle = problemTitle;
        this.categoryName = categoryName;
    }
}