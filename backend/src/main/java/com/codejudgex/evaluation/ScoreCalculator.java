package com.codejudgex.evaluation;

import com.codejudgex.submission.entity.SubmissionResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calculates the total score for a submission.
 *
 * Score = sum of weights for all test cases that were ACCEPTED.
 * Maximum possible score = sum of all test case weights (typically 100).
 */
@Component
public class ScoreCalculator {

    private static final String ACCEPTED = "ACCEPTED";

    /**
     * @param results all SubmissionResult rows for this submission
     * @return total score as integer (0–100 typically)
     */
    public int calculate(List<SubmissionResult> results) {
        if (results == null || results.isEmpty()) return 0;
        return results.stream()
                .filter(r -> ACCEPTED.equals(r.getVerdict()))
                .mapToInt(SubmissionResult::getWeight)
                .sum();
    }

    /**
     * Determines the overall verdict from individual test case results.
     * Priority: COMPILATION_ERROR > INTERNAL_ERROR > WRONG_ANSWER > TLE > MLE > RUNTIME_ERROR > ACCEPTED
     */
    public String overallVerdict(List<SubmissionResult> results) {
        if (results == null || results.isEmpty()) return "INTERNAL_ERROR";

        boolean allAccepted = results.stream().allMatch(r -> ACCEPTED.equals(r.getVerdict()));
        if (allAccepted) return "ACCEPTED";

        return results.stream()
                .map(SubmissionResult::getVerdict)
                .filter(v -> !ACCEPTED.equals(v))
                .findFirst()
                .orElse("WRONG_ANSWER");
    }
}
