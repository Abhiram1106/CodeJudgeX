# CodeJudgeX API Design

## 1. Purpose

This document defines the REST API design standards for **CodeJudgeX**.

The goal is to make the API predictable, secure, versioned, well-documented, and easy to consume from the React frontend or any external API client.

---

## 2. API Style

CodeJudgeX uses:

```text
REST APIs
JSON request/response bodies
JWT-based authentication
Swagger/OpenAPI documentation
Versioned API paths
Standard response format
Standard error format
```

---

## 3. Base URL

Local backend URL:

```text
http://localhost:8080
```

Base API path:

```text
/api/v1
```

Example:

```text
http://localhost:8080/api/v1/auth/login
```

---

## 4. API Versioning

All APIs should be versioned.

Current version:

```text
/api/v1
```

Reason:

```text
Allows future breaking changes without destroying existing clients.
```

Future versions can use:

```text
/api/v2
```

---

## 5. Authentication Header

Protected APIs require JWT access token.

Header:

```http
Authorization: Bearer <access_token>
```

Public APIs:

```text
register
login
refresh token
public contest list optional
health endpoints
Swagger UI in local/dev only
```

---

## 6. Standard Success Response

All successful API responses should follow this structure:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-05-13T10:30:00Z"
}
```

For list responses:

```json
{
  "success": true,
  "message": "Data fetched successfully",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

## 7. Standard Error Response

All API errors should follow this format:

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request payload",
  "details": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

## 8. HTTP Status Code Standards

Use proper HTTP status codes.

```text
200 OK - successful fetch/update
201 Created - resource created
202 Accepted - async job accepted
204 No Content - successful delete/no body
400 Bad Request - invalid request
401 Unauthorized - missing/invalid token
403 Forbidden - insufficient permission
404 Not Found - resource not found
409 Conflict - duplicate/conflicting state
422 Unprocessable Entity - valid JSON but business rule failed
429 Too Many Requests - rate limit exceeded
500 Internal Server Error - unexpected server error
503 Service Unavailable - dependent service unavailable
```

---

## 9. Error Code Standards

Use stable internal error codes.

Examples:

```text
VALIDATION_ERROR
AUTH_INVALID_CREDENTIALS
AUTH_TOKEN_EXPIRED
AUTH_TOKEN_INVALID
ACCESS_DENIED
RESOURCE_NOT_FOUND
DUPLICATE_RESOURCE
CONTEST_NOT_LIVE
CONTEST_ALREADY_ENDED
SUBMISSION_RATE_LIMITED
SUBMISSION_ALREADY_EVALUATED
JUDGE0_UNAVAILABLE
EVALUATION_FAILED
RABBITMQ_PUBLISH_FAILED
INTERNAL_ERROR
```

---

## 10. Pagination Standard

All list APIs should support pagination.

Query params:

```text
page=0
size=20
sort=createdAt,desc
```

Rules:

```text
Default page = 0
Default size = 20
Maximum size = 100
Never return unbounded lists
```

Example:

```http
GET /api/v1/problems?page=0&size=20&sort=createdAt,desc
```

---

## 11. Filtering and Searching

Use query parameters for filters.

Examples:

```http
GET /api/v1/problems?difficulty=MEDIUM&tag=dp
GET /api/v1/contests?status=LIVE
GET /api/v1/submissions?contestId=abc&studentId=xyz&status=ACCEPTED
```

Search parameter:

```text
q
```

Example:

```http
GET /api/v1/problems?q=dynamic programming
```

---

## 12. Request Validation

Use Jakarta Validation annotations in DTOs.

Examples:

```java
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Pattern
```

Validation should happen at controller boundary.

Invalid requests should return:

```text
400 Bad Request
```

---

## 13. API Documentation

Use:

```text
springdoc-openapi
Swagger UI
```

Swagger URL:

```text
http://localhost:8080/swagger-ui
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Every endpoint should document:

```text
summary
description
request body
response body
status codes
required role/permission
error responses
```

---

## 14. Main API Groups

```text
/api/v1/auth
/api/v1/users
/api/v1/problems
/api/v1/test-cases
/api/v1/contests
/api/v1/submissions
/api/v1/leaderboards
/api/v1/plagiarism
/api/v1/notifications
/api/v1/admin
/api/v1/audit-logs
```

---

## 15. Auth APIs

### Register

```http
POST /api/v1/auth/register
```

Request:

```json
{
  "name": "Nishant",
  "email": "nishant@example.com",
  "password": "StrongPassword@123",
  "role": "STUDENT",
  "department": "CSE",
  "year": 4
}
```

Response:

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "uuid",
    "email": "nishant@example.com",
    "role": "STUDENT"
  },
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

### Login

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "nishant@example.com",
  "password": "StrongPassword@123"
}
```

Response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "jwt_access_token",
    "refreshToken": "refresh_token",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "uuid",
      "name": "Nishant",
      "email": "nishant@example.com",
      "role": "STUDENT"
    }
  },
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

### Refresh Token

```http
POST /api/v1/auth/refresh
```

---

### Logout

```http
POST /api/v1/auth/logout
```

---

### Current User

```http
GET /api/v1/auth/me
```

---

## 16. User APIs

```http
GET /api/v1/users/me
PUT /api/v1/users/me
GET /api/v1/admin/users
GET /api/v1/admin/users/{id}
PATCH /api/v1/admin/users/{id}/role
PATCH /api/v1/admin/users/{id}/status
```

Access:

```text
/users/me - authenticated user
/admin/users - ADMIN or SUPER_ADMIN
```

---

## 17. Problem APIs

### Create Problem

```http
POST /api/v1/problems
```

Access:

```text
FACULTY
ADMIN
SUPER_ADMIN
```

Request:

```json
{
  "title": "Two Sum",
  "description": "Given an array of integers...",
  "difficulty": "EASY",
  "inputFormat": "First line contains n...",
  "outputFormat": "Print two indices...",
  "constraintsText": "1 <= n <= 10^5",
  "timeLimitMs": 1000,
  "memoryLimitMb": 256,
  "tags": ["array", "hashmap"]
}
```

---

### Get Problems

```http
GET /api/v1/problems?page=0&size=20&difficulty=EASY&tag=array
```

---

### Get Problem By ID

```http
GET /api/v1/problems/{problemId}
```

Student response must not include hidden test cases.

---

### Update Problem

```http
PUT /api/v1/problems/{problemId}
```

---

### Archive Problem

```http
PATCH /api/v1/problems/{problemId}/archive
```

---

## 18. Test Case APIs

### Add Test Case

```http
POST /api/v1/problems/{problemId}/test-cases
```

Access:

```text
FACULTY
ADMIN
SUPER_ADMIN
```

Request:

```json
{
  "inputData": "5\n1 2 3 4 5",
  "expectedOutput": "15",
  "isSample": false,
  "weight": 10
}
```

---

### Get Sample Test Cases

```http
GET /api/v1/problems/{problemId}/sample-test-cases
```

Access:

```text
Authenticated users
```

---

### Get All Test Cases

```http
GET /api/v1/problems/{problemId}/test-cases
```

Access:

```text
FACULTY
ADMIN
SUPER_ADMIN
```

Important:

```text
Students must never access hidden test cases.
```

---

## 19. Contest APIs

### Create Contest

```http
POST /api/v1/contests
```

Request:

```json
{
  "title": "CSE Placement Mock Test 1",
  "description": "Mock coding assessment for final year students",
  "startTime": "2026-05-20T10:00:00Z",
  "endTime": "2026-05-20T12:00:00Z"
}
```

---

### Add Problem To Contest

```http
POST /api/v1/contests/{contestId}/problems
```

Request:

```json
{
  "problemId": "uuid",
  "points": 100,
  "displayOrder": 1
}
```

---

### Join Contest

```http
POST /api/v1/contests/{contestId}/join
```

Access:

```text
STUDENT
```

---

### Get Contest Details

```http
GET /api/v1/contests/{contestId}
```

---

### Get Contest Problems

```http
GET /api/v1/contests/{contestId}/problems
```

---

## 20. Submission APIs

### Submit Code

```http
POST /api/v1/submissions
```

Access:

```text
STUDENT
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

### Get My Submissions

```http
GET /api/v1/submissions/my?page=0&size=20
```

---

### Get Submission Detail

```http
GET /api/v1/submissions/{submissionId}
```

Access rules:

```text
Student can view own submissions.
Faculty/Admin can view contest submissions.
```

---

### Get Contest Submissions

```http
GET /api/v1/contests/{contestId}/submissions?page=0&size=20
```

Access:

```text
FACULTY
ADMIN
SUPER_ADMIN
```

---

## 21. Leaderboard APIs

### Get Contest Leaderboard

```http
GET /api/v1/contests/{contestId}/leaderboard
```

Response:

```json
{
  "success": true,
  "message": "Leaderboard fetched successfully",
  "data": {
    "contestId": "uuid",
    "entries": [
      {
        "rank": 1,
        "studentId": "uuid",
        "studentName": "Nishant",
        "totalScore": 300,
        "solvedCount": 3,
        "lastSubmissionAt": "2026-05-13T10:30:00Z"
      }
    ]
  },
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

## 22. Plagiarism / Similarity APIs

### Trigger Similarity Check

```http
POST /api/v1/contests/{contestId}/plagiarism/check
```

Access:

```text
FACULTY
ADMIN
SUPER_ADMIN
```

Response:

```json
{
  "success": true,
  "message": "Similarity check queued",
  "data": {
    "contestId": "uuid",
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

### Get Plagiarism Flags

```http
GET /api/v1/contests/{contestId}/plagiarism/flags
```

---

### Review Flag

```http
PATCH /api/v1/plagiarism/flags/{flagId}/review
```

Request:

```json
{
  "status": "CONFIRMED",
  "reviewNote": "Submissions are highly similar. Faculty action required."
}
```

---

## 23. Notification APIs

```http
GET /api/v1/notifications/my
PATCH /api/v1/notifications/{notificationId}/read
PATCH /api/v1/notifications/read-all
```

---

## 24. Admin APIs

```http
GET /api/v1/admin/dashboard
GET /api/v1/admin/users
GET /api/v1/admin/submissions
GET /api/v1/admin/plagiarism-flags
GET /api/v1/admin/system-health
```

Access:

```text
ADMIN
SUPER_ADMIN
```

---

## 25. Audit Log APIs

```http
GET /api/v1/admin/audit-logs?page=0&size=20
GET /api/v1/admin/audit-logs?actorId=uuid&action=SUBMISSION_CREATED
```

Access:

```text
ADMIN
SUPER_ADMIN
```

---

## 26. Idempotency

Some APIs should support idempotency later.

Candidates:

```text
submission creation
payment-like future operations
contest join
notification dispatch
rejudge request
```

Header:

```http
Idempotency-Key: unique-client-generated-key
```

Initial MVP may skip this, but the design should mention it.

---

## 27. Rate Limiting

Rate limit sensitive APIs.

Examples:

```text
login attempts
code submissions
plagiarism check trigger
admin exports
refresh token endpoint
```

Rate limit response:

```json
{
  "success": false,
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "timestamp": "2026-05-13T10:30:00Z"
}
```

HTTP status:

```text
429 Too Many Requests
```

---

## 28. API Security Rules

Rules:

```text
Never expose password_hash.
Never expose refresh token hash.
Never expose hidden test cases to students.
Never return full source code in list APIs.
Never expose internal stack traces.
Validate all request bodies.
Apply role checks on every protected endpoint.
Use CORS allowlist.
Use secure headers.
```

---

## 29. DTO Strategy

Do not expose JPA entities directly.

Use:

```text
Request DTOs
Response DTOs
Mapper classes
MapStruct
```

Example:

```text
CreateProblemRequest
ProblemResponse
SubmissionResponse
LeaderboardEntryResponse
ApiErrorResponse
```

---

## 30. Controller Structure

Recommended structure:

```text
auth/AuthController
user/UserController
problem/ProblemController
testcase/TestCaseController
contest/ContestController
submission/SubmissionController
leaderboard/LeaderboardController
plagiarism/PlagiarismController
notification/NotificationController
admin/AdminController
audit/AuditLogController
```

---

## 31. API Success Criteria

The API design is successful if:

```text
all endpoints are versioned
Swagger docs are available
responses follow a standard format
errors follow a standard format
protected APIs require JWT
role checks are enforced
hidden test cases are protected
submission endpoint returns 202 Accepted
list endpoints are paginated
internal entities are not exposed directly
```

---

## 32. Final Summary

CodeJudgeX APIs should be clean, predictable, documented, secure, and production-inspired.

The most important API design decision is:

```text
Code submission is asynchronous.
```

The frontend submits code, receives a queued submission ID, and later fetches the final result once the evaluation worker completes processing.

