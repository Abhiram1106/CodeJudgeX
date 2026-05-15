# CodeJudgeX Judge0 Integration

## 1. Purpose

This document explains how **CodeJudgeX** integrates with **Judge0 CE** for code execution.

Judge0 CE is used as the self-hosted execution engine for compiling and running submitted code against sample and hidden test cases.

The main goal is:

```text
Do not execute user code directly inside Spring Boot.
Use Judge0 CE as the isolated execution layer.
```

---

## 2. Why Judge0 CE

Judge0 CE is chosen because it is:

```text
open-source
self-hostable
Docker-friendly
built for online judges
supports multiple languages
supports time limits
supports memory limits
returns structured execution results
```

This fits the zero-cost and open-source goal of CodeJudgeX.

---

## 3. Integration Position in Architecture

```text
Student submits code
    ↓
Spring Boot API saves submission
    ↓
RabbitMQ queues evaluation job
    ↓
Evaluation Worker consumes job
    ↓
Worker calls Judge0 CE
    ↓
Judge0 runs code
    ↓
Worker receives result
    ↓
Result saved in PostgreSQL
```

Spring Boot does not compile or run user code directly.

---

## 4. Judge0 Responsibilities

Judge0 is responsible for:

```text
compiling submitted code
executing submitted code
applying time limits
applying memory limits
returning stdout
returning stderr
returning compile errors
returning runtime errors
returning execution status
```

CodeJudgeX is responsible for:

```text
submission validation
fetching test cases
calling Judge0
output comparison
score calculation
status mapping
leaderboard update
result persistence
```

---

## 5. Local Judge0 Setup

Judge0 CE should run through Docker Compose.

Recommended services:

```text
judge0-server
judge0-workers
judge0-db
judge0-redis
```

CodeJudgeX backend should connect to Judge0 using an environment variable:

```text
JUDGE0_BASE_URL=http://judge0-server:2358
```

For local browser/API testing:

```text
http://localhost:2358
```

---

## 6. Backend Configuration

Application config:

```yaml
judge0:
  base-url: ${JUDGE0_BASE_URL:http://localhost:2358}
  timeout-ms: ${JUDGE0_TIMEOUT_MS:10000}
```

Environment variables:

```text
JUDGE0_BASE_URL
JUDGE0_TIMEOUT_MS
```

---

## 7. Backend Package Structure

Recommended package:

```text
infrastructure/judge0
```

Classes:

```text
Judge0Client
Judge0Config
Judge0LanguageMapper
Judge0SubmissionRequest
Judge0SubmissionResponse
Judge0StatusMapper
Judge0Exception
```

---

## 8. Judge0 Client Responsibilities

`Judge0Client` should:

```text
send code execution request
send language id
send stdin
send time limit
send memory limit
poll or wait for result
handle Judge0 errors
handle timeouts
map response to internal DTO
```

The client should hide Judge0 API complexity from the evaluation service.

---

## 9. Language Mapping

CodeJudgeX language names should be mapped to Judge0 language IDs.

Example internal enum:

```text
JAVA
PYTHON
CPP
JAVASCRIPT
```

MVP language support:

```text
JAVA
```

Recommended next languages:

```text
PYTHON
CPP
```

Language mapping should be centralized in:

```text
Judge0LanguageMapper
```

Do not hardcode language IDs throughout the codebase.

---

## 10. Judge0 Execution Request

For each test case, the worker sends:

```text
source_code
language_id
stdin
expected_output optional only if Judge0 comparison is used
time_limit
memory_limit
```

Recommended approach:

```text
Judge0 executes code.
CodeJudgeX compares output itself.
```

This gives CodeJudgeX more control over scoring and output normalization.

---

## 11. Example Judge0 Request Payload

```json
{
  "source_code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }",
  "language_id": 62,
  "stdin": "",
  "cpu_time_limit": 1,
  "memory_limit": 256000
}
```

Note:

```text
language_id is example only. Confirm actual Judge0 language IDs from the running Judge0 instance.
```

---

## 12. Execution Strategy

MVP strategy:

```text
run each test case sequentially
stop early on compilation error
store per-test-case results
calculate weighted score
```

Future strategy:

```text
batch test case execution
parallel execution
custom checker support
language-specific optimization
```

---

## 13. Output Comparison

Judge0 returns actual output.

CodeJudgeX compares:

```text
actual_output
expected_output
```

Basic normalization:

```text
convert CRLF to LF
trim trailing whitespace per line
trim final trailing newline
```

Avoid excessive normalization unless required.

---

## 14. Status Mapping

Judge0 statuses should be mapped to internal CodeJudgeX statuses.

Internal statuses:

```text
ACCEPTED
WRONG_ANSWER
COMPILATION_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
INTERNAL_ERROR
```

Mapping examples:

```text
Judge0 Accepted + output matches → ACCEPTED
Judge0 Accepted + output mismatch → WRONG_ANSWER
Judge0 Compilation Error → COMPILATION_ERROR
Judge0 Runtime Error → RUNTIME_ERROR
Judge0 Time Limit Exceeded → TIME_LIMIT_EXCEEDED
Judge0 Internal Error → INTERNAL_ERROR
```

---

## 15. Time and Memory Limits

Problem entity contains:

```text
time_limit_ms
memory_limit_mb
```

Worker converts these into Judge0 request values.

Example:

```text
1000 ms → 1 second
256 MB → Judge0 memory limit format
```

Rules:

```text
every problem must have time limit
every problem must have memory limit
limits must be validated during problem creation
```

---

## 16. Handling Compilation Errors

If compilation fails:

```text
set submission status = COMPILATION_ERROR
store compiler output safely
skip remaining test cases if compilation is global
update result
create audit log
```

Student can see compiler error, but sensitive system details should be hidden.

---

## 17. Handling Runtime Errors

If runtime error occurs:

```text
mark test case as RUNTIME_ERROR
store safe error message
continue or stop based on strategy
calculate score from passed tests before failure if allowed
```

MVP can stop on first runtime error.

Advanced version can continue per test case.

---

## 18. Handling Time Limit Exceeded

If Judge0 reports TLE:

```text
mark test case as TIME_LIMIT_EXCEEDED
mark submission final status accordingly
store execution time
```

If at least some tests pass and later test TLE occurs:

```text
PARTIALLY_ACCEPTED or TIME_LIMIT_EXCEEDED based on scoring policy
```

Recommended MVP:

```text
If any test TLE occurs, final status = TIME_LIMIT_EXCEEDED unless all previous score rules decide otherwise.
```

---

## 19. Judge0 Failure Handling

Judge0 may fail due to:

```text
server unavailable
worker unavailable
request timeout
bad response
internal error
resource exhaustion
```

Handling:

```text
retry evaluation job if system failure
move to DLQ after retry limit
mark submission as INTERNAL_ERROR
log error with requestId/submissionId
```

Do not mark user code as wrong if Judge0 itself failed.

---

## 20. Security Rules

Rules:

```text
never execute submitted code in Spring Boot
never expose Judge0 directly to public users
never log full source code by default
limit source code size
limit output size
sanitize error messages
use Docker network isolation
protect Judge0 service behind internal Docker network
```

---

## 21. Judge0 API Exposure

Recommended:

```text
Judge0 should be accessible only by backend/worker inside Docker network.
```

Avoid:

```text
public frontend → Judge0 direct calls
```

Correct flow:

```text
frontend → Spring Boot API → worker → Judge0
```

---

## 22. Per-Test-Case Result Model

For each test case, store:

```text
test_case_id
status
actual_output
expected_output restricted
execution_time_ms
memory_used_mb
error_message
```

Student visibility:

```text
sample test details visible
hidden test details hidden
verdict visible
```

Faculty/admin visibility:

```text
full test case result visible if permitted
```

---

## 23. Observability

Track Judge0-related metrics:

```text
judge0_requests_total
judge0_success_total
judge0_error_total
judge0_timeout_total
judge0_request_duration_ms
judge0_internal_error_count
```

Logs should include:

```text
requestId
submissionId
contestId
problemId
language
judge0Status
durationMs
```

Do not log:

```text
full source code
hidden test input/output
secrets
```

---

## 24. Testing Strategy

Test:

```text
language mapping works
Judge0 request is built correctly
Judge0 response maps to internal verdict
compilation error handled correctly
runtime error handled correctly
TLE handled correctly
output comparison works
Judge0 unavailable triggers retry/internal error
```

Use mocks for unit tests.

Use real Judge0 container only for integration tests if feasible.

---

## 25. MVP Scope

MVP Judge0 integration should include:

```text
Java language support
single submission execution
sample + hidden test case execution
basic output comparison
compilation error handling
runtime error handling
time limit handling
score calculation
```

---

## 26. Advanced Scope

Later add:

```text
Python support
C++ support
batch submissions
parallel test execution
custom checker
floating point tolerance
language templates
Judge0 health dashboard
execution history
```

---

## 27. Success Criteria

Judge0 integration is successful if:

```text
Spring Boot never runs user code directly
worker can send code to Judge0
Judge0 returns execution result
outputs are compared correctly
hidden tests are evaluated
compilation/runtime/TLE errors are handled
final submission status is accurate
results are saved in PostgreSQL
leaderboard updates after evaluation
Judge0 failures do not corrupt submissions
```

---

## 28. Final Summary

Judge0 CE is the execution backbone of CodeJudgeX.

The most important rule is:

```text
Judge0 executes code. CodeJudgeX evaluates meaning, scoring, persistence, security, and workflow.
```

This separation keeps the platform safer, cleaner, and easier to extend.

