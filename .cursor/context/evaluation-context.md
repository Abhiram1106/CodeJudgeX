# Evaluation Pipeline Context — CodeJudgeX

> @-include this file when working on submission, evaluation, or Judge0 integration.

## The full async pipeline

```
Student (browser)
  ↓ POST /api/v1/submissions {contestId, problemId, language, sourceCode}
SubmissionController
  ↓ validate JWT + contest access + payload
SubmissionService
  ↓ save Submission{status: QUEUED} → PostgreSQL
  ↓ publish EvaluationJobDto → RabbitMQ "evaluation.queue"
  ↓ return {submissionId, status: QUEUED} HTTP 202 Accepted

--- async boundary ---

EvaluationWorker @RabbitListener("evaluation.queue")
  ↓ fetch Submission from PostgreSQL
  ↓ fetch Problem (time/memory limits) from PostgreSQL
  ↓ fetch ALL TestCases (including hidden) from PostgreSQL
  ↓ update Submission{status: RUNNING}
  ↓ for each TestCase:
      POST /submissions to Judge0 CE {source_code, language_id, stdin, time_limit, memory_limit}
      poll Judge0 until status is terminal
      compare stdout with expected_output
      record SubmissionResult{testCaseId, actualOutput, status, executionTimeMs, memoryUsedMb}
  ↓ calculate total score (sum of weights of passing test cases)
  ↓ determine final verdict (ACCEPTED only if ALL test cases pass)
  ↓ update Submission{status: <verdict>, score}
  ↓ update Redis leaderboard: ZADD contest:{contestId}:leaderboard score userId
  ↓ save LeaderboardSnapshot (if ACCEPTED or score improved)
  ↓ publish NotificationJobDto → RabbitMQ "notification.queue"
  ↓ ack message

--- error paths ---

If Judge0 unavailable:
  → nack + requeue to "evaluation.retry" queue (with TTL → DLQ after 3 attempts)
  → Submission{status: EVALUATION_ERROR}

If worker crashes mid-evaluation:
  → RabbitMQ re-delivers the unacked message to another worker instance
  → Idempotency check: if status is already ACCEPTED/WRONG_ANSWER/etc., skip re-evaluation
```

## Judge0 CE integration

**Base URL:** `${JUDGE0_URL}` (default: `http://localhost:2358`)
**Auth:** `X-Auth-Token: ${JUDGE0_TOKEN}` header (empty for CE without auth)

**Submit for execution:**
```
POST /submissions?wait=false
{
  "source_code": "<base64 encoded>",
  "language_id": 62,       // Java: 62, Python3: 71, C++17: 54, JS: 63
  "stdin": "<base64>",
  "expected_output": "<base64>",
  "cpu_time_limit": 1.0,   // seconds
  "memory_limit": 262144   // KB (256 MB)
}
→ { "token": "judge0-submission-token" }

GET /submissions/{token}
→ { "status": { "id": 3, "description": "Accepted" }, "stdout": "...", "stderr": "...", "time": "0.123", "memory": 1024 }
```

**Judge0 status IDs:**

| ID | Description | Our verdict |
|---|---|---|
| 1 | In Queue | QUEUED |
| 2 | Processing | RUNNING |
| 3 | Accepted | (test case pass) |
| 4 | Wrong Answer | WRONG_ANSWER |
| 5 | Time Limit Exceeded | TIME_LIMIT_EXCEEDED |
| 6 | Compilation Error | COMPILATION_ERROR |
| 7–12 | Various runtime errors | RUNTIME_ERROR |
| 13 | Internal Error | EVALUATION_ERROR |
| 14 | Exec Format Error | EVALUATION_ERROR |

**Polling:** Poll `GET /submissions/{token}` every 500ms until `status.id >= 3`. Set a max 30s timeout.

## Language IDs (Judge0)

| Language | ID |
|---|---|
| Java (OpenJDK 17) | 62 |
| C++ (GCC 17) | 54 |
| Python 3 | 71 |
| JavaScript (Node.js) | 63 |
| C (GCC) | 50 |

## RabbitMQ configuration

```java
// Queue names (define as constants in infrastructure/)
public static final String EVALUATION_QUEUE = "evaluation.queue";
public static final String EVALUATION_RETRY  = "evaluation.retry";
public static final String EVALUATION_DLQ    = "evaluation.dlq";
public static final String NOTIFICATION_QUEUE = "notification.queue";
public static final String PLAGIARISM_QUEUE   = "plagiarism.queue";

// Retry policy: TTL 30s on retry queue → routes to DLQ after 3 nacks
```

## Idempotency rule

Before evaluating, check if the submission already has a terminal status.
If it does, ack the message and return — do not re-evaluate.
This prevents duplicate verdicts if RabbitMQ re-delivers an already-processed message.
