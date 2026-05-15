# CodeJudgeX Submission Evaluation Design

## 1. Purpose

This document explains the submission evaluation workflow for **CodeJudgeX**.

Submission evaluation is the core feature of the platform. It is what separates CodeJudgeX from a basic CRUD project.

The main goal is:

```text
Student submits code → system evaluates it asynchronously → result is saved → leaderboard updates
```

---

## 2. Core Principle

The most important rule:

```text
Never evaluate code inside the API request.
```

The API should only:

```text
validate request
save submission as QUEUED
publish evaluation job to RabbitMQ
return 202 Accepted
```

Actual evaluation happens in a background worker.

---

## 3. High-Level Flow

```text
Student submits code
    ↓
Spring Boot API validates submission
    ↓
Submission saved as QUEUED
    ↓
Evaluation job sent to RabbitMQ
    ↓
Evaluation worker consumes job
    ↓
Worker fetches submission and test cases
    ↓
Worker sends code to Judge0 CE
    ↓
Judge0 compiles/runs code
    ↓
Worker compares actual output with expected output
    ↓
Score and verdict are calculated
    ↓
Submission result saved in PostgreSQL
    ↓
Leaderboard updated in Redis
```

---

## 4. Submission Lifecycle

Submission statuses:

```text
QUEUED
RUNNING
ACCEPTED
WRONG_ANSWER
PARTIALLY_ACCEPTED
COMPILATION_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
INTERNAL_ERROR
```

---

## 5. Status Transition Rules

Allowed transitions:

```text
QUEUED → RUNNING
RUNNING → ACCEPTED
RUNNING → WRONG_ANSWER
RUNNING → PARTIALLY_ACCEPTED
RUNNING → COMPILATION_ERROR
RUNNING → RUNTIME_ERROR
RUNNING → TIME_LIMIT_EXCEEDED
RUNNING → MEMORY_LIMIT_EXCEEDED
RUNNING → INTERNAL_ERROR
```

Do not allow:

```text
ACCEPTED → RUNNING
WRONG_ANSWER → RUNNING
COMPILATION_ERROR → RUNNING
```

Exception:

```text
Rejudge feature may allow final status → QUEUED later.
```

---

## 6. Submission API Behavior

Endpoint:

```http
POST /api/v1/submissions
```

Request:

```json
{
  "contestId": "uuid",
  "problemId": "uuid",
  "language": "JAVA",
  "sourceCode": "public class Main { public static void main(String[] args) { } }"
}
```

Response:

```json
{
  "success": true,
  "message": "Submission queued for evaluation",
  "data": {
    "submissionId": "uuid",
    "status": "QUEUED"
  },
  "timestamp": "2026-05-13T10:30:00Z"
}
```

HTTP status:

```text
202 Accepted
```

---

## 7. Submission Validation

Before accepting a submission, validate:

```text
user is authenticated
user role is STUDENT
contest exists
contest is LIVE
student has joined contest
problem belongs to contest
language is allowed
source code is not empty
source code size is within limit
submission rate limit is not exceeded
```

If validation passes:

```text
save submission as QUEUED
publish evaluation job
```

---

## 8. Source Code Limits

Recommended limits:

```text
max source code size: 100 KB for MVP
max submissions per minute: configurable
allowed languages: JAVA first
```

Future:

```text
Python
C++
JavaScript
```

---

## 9. Submission Record

When a submission is created, store:

```text
submission_id
student_id
contest_id
problem_id
language
source_code
source_code_hash
status = QUEUED
score = 0
submitted_at
created_at
updated_at
```

Important:

```text
submitted source code should be immutable.
```

---

## 10. RabbitMQ Evaluation Job

Queue:

```text
submission.evaluation.queue
```

Message payload:

```json
{
  "submissionId": "uuid",
  "contestId": "uuid",
  "problemId": "uuid",
  "studentId": "uuid",
  "attempt": 1,
  "createdAt": "2026-05-13T10:30:00Z"
}
```

Do not put full source code in the queue unless required.

Preferred approach:

```text
queue contains IDs
worker fetches full data from PostgreSQL
```

---

## 11. Evaluation Worker Responsibilities

The worker should:

```text
consume RabbitMQ message
validate submission still exists
check submission status is QUEUED
mark submission as RUNNING
fetch source code
fetch problem test cases
call Judge0 CE
process result
calculate score
save submission results
update final submission status
update Redis leaderboard
create audit log
acknowledge RabbitMQ message
```

---

## 12. Judge0 Evaluation Strategy

For each test case:

```text
send source code
send language id
send stdin
set time limit
set memory limit
receive stdout/stderr/status
compare output
store result
```

Initial MVP can evaluate test cases sequentially.

Future improvement:

```text
batch submissions to Judge0
parallel test case execution
```

---

## 13. Output Comparison

Basic comparison strategy:

```text
trim trailing spaces
normalize line endings
compare exact output
```

Recommended normalization:

```text
convert CRLF to LF
trim trailing whitespace per line
trim final trailing newline
```

Do not over-normalize unless problem requires it.

Future:

```text
custom checker
floating point tolerance checker
case-insensitive checker optional
```

---

## 14. Scoring Logic

Each test case has a weight.

Example:

```text
sample tests: low weight
hidden tests: high weight
edge tests: high weight
```

Score calculation:

```text
score = sum(weights of passed test cases)
```

Final verdict:

```text
all tests passed → ACCEPTED
some tests passed → PARTIALLY_ACCEPTED
no tests passed due to wrong output → WRONG_ANSWER
compile failed → COMPILATION_ERROR
runtime failed → RUNTIME_ERROR
time exceeded → TIME_LIMIT_EXCEEDED
memory exceeded → MEMORY_LIMIT_EXCEEDED
system failure → INTERNAL_ERROR
```

---

## 15. Submission Result Storage

For every test case, store:

```text
submission_id
test_case_id
status
actual_output
expected_output optional/restricted
execution_time_ms
memory_used_mb
error_message
created_at
```

Student view should not expose hidden test input/output.

Faculty/admin view may expose details based on permission.

---

## 16. Leaderboard Update

After final verdict:

```text
calculate best score for student/problem
update contest total score
update solved count
update last submission time
update Redis sorted set
update PostgreSQL leaderboard snapshot
```

Important:

```text
Only best score per problem should count.
```

Example:

```text
Student submits 40/100 first
Student submits 100/100 later
Leaderboard should count 100
```

---

## 17. Redis Leaderboard Key

Key:

```text
leaderboard:contest:{contestId}
```

Member:

```text
studentId
```

Score:

```text
totalScore
```

Additional metadata can be stored in PostgreSQL.

---

## 18. Idempotency

RabbitMQ may deliver duplicate messages.

Worker must check:

```text
submission exists
submission status is QUEUED before processing
submission is not already final
message attempt count
```

If duplicate message arrives for completed submission:

```text
ignore safely
acknowledge message
```

---

## 19. Retry Strategy

Retry only for system failures.

Retry examples:

```text
Judge0 unavailable
temporary DB connection issue
RabbitMQ transient issue
network timeout
```

Do not retry for:

```text
wrong answer
compilation error
runtime error caused by user code
time limit exceeded caused by user code
```

---

## 20. Dead-Letter Queue

If a message fails repeatedly:

```text
move to dead-letter queue
mark submission as INTERNAL_ERROR
store error reason
notify/admin log the failure
```

DLQ:

```text
dead.letter.queue
```

---

## 21. Failure Scenarios

## 21.1 Judge0 Unavailable

Handling:

```text
retry evaluation job
if retries exhausted, mark INTERNAL_ERROR
send to DLQ
```

---

## 21.2 Worker Crash

Handling:

```text
message should not be acknowledged before completion
RabbitMQ redelivers message
worker must be idempotent
```

---

## 21.3 Database Failure

Handling:

```text
do not acknowledge message if result cannot be saved
retry later
```

---

## 21.4 Redis Failure

Handling:

```text
save result in PostgreSQL
log Redis failure
rebuild leaderboard later from PostgreSQL
```

---

## 22. Audit Logging

Create audit logs for:

```text
SUBMISSION_CREATED
SUBMISSION_EVALUATION_STARTED
SUBMISSION_EVALUATED
SUBMISSION_EVALUATION_FAILED
LEADERBOARD_UPDATED
```

Audit metadata:

```text
submissionId
studentId
contestId
problemId
status
score
executionTimeMs
```

---

## 23. Metrics

Track:

```text
submissions_created_total
submissions_queued_total
submissions_running_total
submissions_accepted_total
submissions_failed_total
evaluation_duration_ms
judge0_request_duration_ms
judge0_error_count
rabbitmq_evaluation_queue_depth
evaluation_retry_count
evaluation_dlq_count
```

---

## 24. Frontend Submission UX

After submit:

```text
show "Submission queued"
redirect to result page
poll status every 2-3 seconds
show RUNNING state
stop polling when final status is reached
show verdict clearly
update leaderboard link
```

Do not freeze the UI while waiting.

---

## 25. Security Rules

Rules:

```text
students can submit only for live contests
students can view only own source code
hidden test case data must not be shown to students
source code should not be logged
Judge0 should handle execution, not Spring Boot
limit source code size
rate limit submissions
```

---

## 26. MVP Scope

MVP should support:

```text
Java submissions only
sample + hidden test cases
RabbitMQ queue
Judge0 CE integration
basic scoring
basic Redis leaderboard
basic polling UI
basic error statuses
```

---

## 27. Advanced Scope

Later add:

```text
Python and C++ support
custom checker
floating point tolerance
parallel test execution
rejudge submissions
leaderboard freeze
submission diff viewer
language-specific templates
editor auto-save
```

---

## 28. Success Criteria

Submission evaluation is successful if:

```text
API returns 202 quickly
submission is saved as QUEUED
RabbitMQ receives job
worker consumes job
Judge0 executes code
hidden tests are evaluated
result is saved correctly
leaderboard updates
failures are retried or sent to DLQ
duplicate messages do not corrupt results
student sees final verdict
```

---

## 29. Final Summary

Submission evaluation is the heart of CodeJudgeX.

The most important engineering decision is:

```text
Code evaluation must be asynchronous, isolated, observable, and recoverable.
```

If this workflow is built cleanly, CodeJudgeX becomes a serious engineering project instead of a basic coding dashboard.

