package com.codejudgex.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "submission_results")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "test_case_id", nullable = false)
    private UUID testCaseId;

    /** Overall status for this test case run (mirrors Judge0 mapped verdict). */
    @Column(nullable = false, length = 30)
    private String status;

    /** Human-readable verdict: ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, etc. */
    @Column(nullable = false, length = 30)
    private String verdict;

    /** Weight of this test case (0–100). Populated from TestCase.weight at evaluation time. */
    @Column(nullable = false)
    private int weight;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
