package com.codejudgex.evaluation;

/**
 * Thrown when evaluation fails due to Judge0 errors, polling exhaustion,
 * or unexpected internal state. Triggers the retry/DLQ mechanism in EvaluationWorker.
 */
public class EvaluationException extends RuntimeException {

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
