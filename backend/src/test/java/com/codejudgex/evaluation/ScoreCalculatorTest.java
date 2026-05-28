package com.codejudgex.evaluation;

import com.codejudgex.submission.entity.SubmissionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    private SubmissionResult result(String verdict, int weight) {
        SubmissionResult r = new SubmissionResult();
        r.setVerdict(verdict);
        r.setWeight(weight);
        r.setStatus(verdict);
        return r;
    }

    @Test
    void allAccepted_returnsSumOfWeights() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 40),
                result("ACCEPTED", 60)
        );
        assertThat(calculator.calculate(results)).isEqualTo(100);
    }

    @Test
    void partialAccepted_returnsSumOfAcceptedWeightsOnly() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 40),
                result("WRONG_ANSWER", 60)
        );
        assertThat(calculator.calculate(results)).isEqualTo(40);
    }

    @Test
    void noneAccepted_returnsZero() {
        List<SubmissionResult> results = List.of(
                result("WRONG_ANSWER", 50),
                result("TIME_LIMIT_EXCEEDED", 50)
        );
        assertThat(calculator.calculate(results)).isEqualTo(0);
    }

    @Test
    void emptyResults_returnsZero() {
        assertThat(calculator.calculate(List.of())).isEqualTo(0);
    }

    @Test
    void overallVerdict_allAccepted_returnsAccepted() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 50),
                result("ACCEPTED", 50)
        );
        assertThat(calculator.overallVerdict(results)).isEqualTo("ACCEPTED");
    }

    @Test
    void overallVerdict_mixedResults_returnsFirstNonAccepted() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 50),
                result("WRONG_ANSWER", 50)
        );
        assertThat(calculator.overallVerdict(results)).isEqualTo("WRONG_ANSWER");
    }

    @Test
    void overallVerdict_emptyResults_returnsInternalError() {
        assertThat(calculator.overallVerdict(List.of())).isEqualTo("INTERNAL_ERROR");
    }
}
