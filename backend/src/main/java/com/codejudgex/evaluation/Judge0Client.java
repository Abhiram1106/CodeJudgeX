package com.codejudgex.evaluation;

import com.codejudgex.evaluation.dto.Judge0SubmissionRequest;
import com.codejudgex.evaluation.dto.Judge0SubmissionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.Map;

/**
 * HTTP client wrapper for the Judge0 CE REST API.
 *
 * All source code, stdin, and stdout are base64-encoded per Judge0 contract.
 * Polling is synchronous with configurable interval and max attempts.
 */
@Slf4j
@Component
public class Judge0Client {

    private static final String SUBMIT_PATH = "/submissions";
    private static final String POLL_PATH   = "/submissions/{token}";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final long pollIntervalMs;
    private final int maxPollAttempts;

    public Judge0Client(
            RestTemplate judge0RestTemplate,
            @Value("${app.judge0.base-url}") String baseUrl,
            @Value("${app.judge0.poll-interval-ms}") long pollIntervalMs,
            @Value("${app.judge0.max-poll-attempts}") int maxPollAttempts) {
        this.restTemplate   = judge0RestTemplate;
        this.baseUrl        = baseUrl;
        this.pollIntervalMs = pollIntervalMs;
        this.maxPollAttempts = maxPollAttempts;
    }

    /**
     * Submit source code for a single test case and poll until a terminal verdict.
     *
     * @param sourceCode  raw source code (will be base64-encoded before sending)
     * @param languageId  Judge0 language ID (e.g. 62 = Java)
     * @param stdin       raw stdin string (will be base64-encoded)
     * @param timeLimitMs time limit in milliseconds (converted to seconds for Judge0)
     * @param memoryLimitMb memory limit in megabytes (converted to KB for Judge0)
     * @return terminal Judge0 response with verdict, stdout, stderr
     * @throws EvaluationException if the token is never obtained or polling exhausted
     */
    public Judge0SubmissionResponse submitAndPoll(
            String sourceCode,
            int languageId,
            String stdin,
            int timeLimitMs,
            int memoryLimitMb) {

        String token = submit(sourceCode, languageId, stdin, timeLimitMs, memoryLimitMb);
        return poll(token);
    }

    // --- private helpers ---

    private String submit(String sourceCode, int languageId, String stdin, int timeLimitMs, int memoryLimitMb) {
        Judge0SubmissionRequest body = Judge0SubmissionRequest.builder()
                .sourceCode(base64(sourceCode))
                .languageId(languageId)
                .stdin(base64(stdin != null ? stdin : ""))
                .cpuTimeLimit(timeLimitMs / 1000.0)
                .memoryLimit(memoryLimitMb * 1024)
                .build();

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + SUBMIT_PATH)
                .queryParam("base64_encoded", "true")
                .queryParam("wait", "false")
                .toUriString();

        Judge0SubmissionResponse response = restTemplate.postForObject(url, body, Judge0SubmissionResponse.class);
        if (response == null || response.getToken() == null) {
            throw new EvaluationException("Judge0 did not return a submission token");
        }

        log.debug("Judge0 token obtained: {}", response.getToken());
        return response.getToken();
    }

    private Judge0SubmissionResponse poll(String token) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + POLL_PATH)
                .queryParam("base64_encoded", "true")
                .toUriString();

        for (int attempt = 1; attempt <= maxPollAttempts; attempt++) {
            Judge0SubmissionResponse response = restTemplate.getForObject(
                    url, Judge0SubmissionResponse.class, Map.of("token", token));

            if (response == null) {
                throw new EvaluationException("Judge0 returned null response for token: " + token);
            }

            int statusId = response.getStatus() != null ? response.getStatus().getId() : 0;
            log.debug("Judge0 poll attempt {}/{} — token={} status={}", attempt, maxPollAttempts, token, statusId);

            if (Judge0StatusMapper.isTerminal(statusId)) {
                return response;
            }

            sleep(pollIntervalMs);
        }

        throw new EvaluationException("Judge0 polling exhausted for token: " + token);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EvaluationException("Polling interrupted");
        }
    }
}
