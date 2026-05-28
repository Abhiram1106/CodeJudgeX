package com.codejudgex.contest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "contest_problems")
@Getter
@Setter
@NoArgsConstructor
public class ContestProblem {

    @EmbeddedId
    private ContestProblemId id = new ContestProblemId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contestId")
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(name = "problem_order")
    private int problemOrder = 0;

    public UUID getProblemId() {
        return id.getProblemId();
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContestProblemId implements Serializable {
        @Column(name = "contest_id")
        private UUID contestId;

        @Column(name = "problem_id")
        private UUID problemId;
    }
}
