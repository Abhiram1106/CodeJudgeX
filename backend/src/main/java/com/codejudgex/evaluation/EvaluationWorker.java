package com.codejudgex.evaluation;

import com.codejudgex.evaluation.dto.Judge0SubmissionResponse;
import com.codejudgex.infrastructure.config.RabbitMQConfig;
import com.codejudgex.problem.entity.Problem;
import com.codejudgex.problem.entity.TestCase;
import com.codejudgex.problem.repository.ProblemRepository;
import com.codejudgex.problem.repository.TestCaseRepository;
import com.codejudgex.submission.dto.EvaluationMessage;
import com.codejudgex.submission.entity.Submission;
import com.codejudgex.submission.entity.SubmissionResult;
import com.codejudgex.submission.repository.SubmissionRepository;
import com.codejudgex.leaderboard.service.LeaderboardService;
import com.codejudgex.submission.repository.SubmissionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Asynchronous evaluation worker.
 *
 * Flow:
 *   1. Consume EvaluationMessage from evaluation.queue
 *   2. Set submission status → RUNNING
 *   3. Load all test cases for the problem (hidden + sample)
 *   4. For each test case: submit to Judge0, decode stdout, compare with expected
 *   5. Persist SubmissionResult per test case
 *   6. Calculate total score and overall verdict
 *   7. Update Submission with final status + score + evaluatedAt
 *   8. On any exception: increment attemptNumber, retry via retry queue (max 3), then DLQ + INTERNAL_ERROR
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationWorker {

    private static final int MAX_ATTEMPTS = 3;

    private final Judge0Client         judge0Client;
    private final OutputComparator     outputComparator;
    private final ScoreCalculator      scoreCalculator;
    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final ProblemRepository    problemRepository;
    private final TestCaseRepository   testCaseRepository;
    private final LeaderboardService   leaderboardService;
    private final RabbitTemplate       rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.EVALUATION_QUEUE)
    public void onEvaluationMessage(EvaluationMessage message) {
        log.info("Evaluation started — submissionId={} attempt={}", message.getSubmissionId(), message.getAttemptNumber());

        Submission submission = submissionRepository.findById(message.getSubmissionId())
                .orElseThrow(() -> new EvaluationException("Submission not found: " + message.getSubmissionId()));

        try {
            evaluate(submission, message);
        } catch (Exception ex) {
            log.error("Evaluation failed — submissionId={} attempt={}", message.getSubmissionId(), message.getAttemptNumber(), ex);
            handleFailure(submission, message, ex);
        }
    }

    @Transactional
    protected void evaluate(Submission submission, EvaluationMessage message) {
        // 1. Mark RUNNING
        submission.setStatus("RUNNING");
        submissionRepository.save(submission);

        // 2. Load problem + all test cases
        Problem problem = problemRepository.findById(message.getProblemId())
                .orElseThrow(() -> new EvaluationException("Problem not found: " + message.getProblemId()));

        List<TestCase> testCases = testCaseRepository.findByProblemId(message.getProblemId());
        if (testCases.isEmpty()) {
            throw new EvaluationException("No test cases found for problem: " + message.getProblemId());
        }

        // 3. Evaluate each test case
        List<SubmissionResult> results = new ArrayList<>();
        String firstNonAcceptedVerdict = null;

        for (TestCase testCase : testCases) {
            SubmissionResult result = evaluateTestCase(submission, testCase, message, problem);
            results.add(result);

            if (firstNonAcceptedVerdict == null && !"ACCEPTED".equals(result.getVerdict())) {
                firstNonAcceptedVerdict = result.getVerdict();
            }
        }

        // 4. Persist results
        submissionResultRepository.saveAll(results);

        // 5. Calculate score and overall verdict
        int totalScore   = scoreCalculator.calculate(results);
        String verdict   = scoreCalculator.overallVerdict(results);

        // 6. Update submission
        submission.setStatus(verdict);
        submission.setScore(totalScore);
        submission.setEvaluatedAt(Instant.now());
        submissionRepository.save(submission);

        log.info("Evaluation complete — submissionId={} verdict={} score={}", submission.getId(), verdict, totalScore);

        // Update Redis leaderboard (best score wins — handled by ZADD which overwrites only if higher)
        if ("ACCEPTED".equals(verdict) || totalScore > 0) {
            leaderboardService.updateScore(submission.getContestId(), submission.getStudentId(), totalScore);
        }
    }

    private SubmissionResult evaluateTestCase(
            Submission submission, TestCase testCase, EvaluationMessage message, Problem problem) {

        Judge0SubmissionResponse judge0Response = judge0Client.submitAndPoll(
                message.getSourceCode(),
                message.getLanguageId(),
                testCase.getInputData(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb()
        );

        int statusId = judge0Response.getStatus() != null ? judge0Response.getStatus().getId() : 13;
        String mappedVerdict = Judge0StatusMapper.map(statusId);

        // For ACCEPTED status from Judge0, verify output ourselves
        if ("ACCEPTED".equals(mappedVerdict)) {
            String actualOutput = decodeBase64(judge0Response.getStdout());
            if (!outputComparator.isCorrect(actualOutput, testCase.getExpectedOutput())) {
                mappedVerdict = "WRONG_ANSWER";
            }
        }

        SubmissionResult result = new SubmissionResult();
        result.setSubmission(submission);
        result.setTestCaseId(testCase.getId());
        result.setStatus(mappedVerdict);
        result.setVerdict(mappedVerdict);
        result.setWeight(testCase.getWeight());
        result.setActualOutput(decodeBase64(judge0Response.getStdout()));
        result.setExecutionTimeMs(parseTimeMs(judge0Response.getTime()));
        result.setMemoryUsedKb(judge0Response.getMemory());

        String errorMessage = buildErrorMessage(judge0Response);
        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
        }

        return result;
    }

    private void handleFailure(Submission submission, EvaluationMessage message, Exception ex) {
        if (message.getAttemptNumber() < MAX_ATTEMPTS) {
            log.warn("Retrying evaluation — submissionId={} nextAttempt={}", message.getSubmissionId(), message.getAttemptNumber() + 1);
            EvaluationMessage retry = EvaluationMessage.builder()
                    .submissionId(message.getSubmissionId())
                    .problemId(message.getProblemId())
                    .contestId(message.getContestId())
                    .studentId(message.getStudentId())
                    .languageId(message.getLanguageId())
                    .sourceCode(message.getSourceCode())
                    .attemptNumber(message.getAttemptNumber() + 1)
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EVALUATION_RETRY, retry);
        } else {
            log.error("Evaluation exhausted retries — submissionId={} sending to DLQ", message.getSubmissionId());
            submission.setStatus("INTERNAL_ERROR");
            submission.setEvaluatedAt(Instant.now());
            submissionRepository.save(submission);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EVALUATION_DLQ, message);
        }
    }

    private static String decodeBase64(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException e) {
            // Not base64 (Judge0 sometimes returns plain text) — return as-is
            return encoded;
        }
    }

    private static Integer parseTimeMs(String timeSeconds) {
        if (timeSeconds == null || timeSeconds.isBlank()) return null;
        try {
            return (int) (Double.parseDouble(timeSeconds) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String buildErrorMessage(Judge0SubmissionResponse response) {
        String stderr  = decodeBase64(response.getStderr());
        String compile = decodeBase64(response.getCompileOutput());
        if (!stderr.isBlank()) return stderr;
        if (!compile.isBlank()) return compile;
        return null;
    }
}
