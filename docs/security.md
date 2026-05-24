# CodeJudgeX Security Design

## 1. Purpose

This document defines the security design for **CodeJudgeX**.

CodeJudgeX handles sensitive data such as user accounts, roles, hidden test cases, submitted source code, contest results, plagiarism flags, and audit logs. Security must be treated as a core system requirement, not an afterthought.

---

## 2. Security Goals

The security goals of CodeJudgeX are:

```text
Protect user accounts
Protect hidden test cases
Protect submitted source code
Prevent unauthorized role access
Prevent fake or abusive submissions
Preserve score/result integrity
Maintain complete auditability
Reduce risk from executing user code
Prevent API abuse
Protect system availability during contests
```

---

## 3. Security Model

CodeJudgeX security is designed around:

```text
Authentication
Authorization
Confidentiality
Integrity
Availability
Accountability
Secure code execution
Rate limiting
Audit logging
Input validation
```

---

## 4. CIA Security Principles

## 4.1 Confidentiality

Confidentiality means only authorized users can access sensitive information.

Protected data:

```text
password hashes
JWT secrets
refresh tokens
hidden test cases
private contest data
student submissions
plagiarism reports
admin actions
audit logs
```

Controls:

```text
BCrypt password hashing
JWT authentication
role-based access control
permission checks
hidden test case access restrictions
sensitive field masking
environment variables
CORS allowlist
private admin APIs
```

---

## 4.2 Integrity

Integrity means data must remain correct, trusted, and tamper-resistant.

Protected data:

```text
submissions
scores
leaderboard results
test cases
contest rules
user roles
plagiarism flags
audit logs
```

Controls:

```text
database constraints
transactions
submission immutability
source code hashing
idempotent queue consumers
audit logging
input validation
role-based modification controls
Flyway migrations
```

---

## 4.3 Availability

Availability means the platform should remain usable during contests and recover from failures.

Controls:

```text
RabbitMQ async processing
retry queues
dead-letter queues
Redis caching
rate limiting
health checks
Docker restart policies
graceful error handling
Prometheus/Grafana monitoring
```

---

## 5. Authentication Design

## 5.1 Phase 1 Authentication

Use:

```text
Spring Security
JWT access tokens
Refresh tokens
BCrypt password hashing
```

Authentication flow:

```text
User submits email/password
    ↓
Spring Security validates credentials
    ↓
Password checked using BCrypt
    ↓
Access token generated
    ↓
Refresh token generated
    ↓
Login audit event stored
    ↓
Tokens returned to client
```

---

## 5.2 Password Security

Rules:

```text
Never store plain text passwords.
Use BCrypt hashing.
Enforce minimum password length.
Reject weak passwords.
Do not log passwords.
Do not return password hashes in APIs.
```

Recommended password policy:

```text
minimum 8 characters
at least one uppercase letter
at least one lowercase letter
at least one number
at least one special character
```

---

## 5.3 JWT Access Tokens

Access token rules:

```text
short expiry time
signed with strong secret
include user id
include role/permissions
validate on every protected request
never store token in database unless blacklisting is used
```

Recommended expiry:

```text
15 minutes
```

---

## 5.4 Refresh Tokens

Refresh token rules:

```text
longer expiry than access token
stored hashed in database
rotated on refresh
revoked on logout
revoked if suspicious activity is detected
```

Recommended expiry:

```text
7 days
```

---

## 5.5 Logout

Logout should:

```text
revoke refresh token
optionally blacklist access token until expiry
create audit log entry
```

Optional Redis key:

```text
jwt_blacklist:{tokenId}
```

---

## 6. Authorization Design

## 6.1 Roles

CodeJudgeX roles:

```text
STUDENT
FACULTY
ADMIN
SUPER_ADMIN
```

---

## 6.2 Role Permissions

### Student

Can:

```text
view contests
join contests
view problems in contests
submit code
view own submissions
view leaderboard
view own notifications
```

Cannot:

```text
create problems
view hidden test cases
view other students' private submissions
review plagiarism flags
manage users
```

---

### Faculty

Can:

```text
create problems
manage own problems
add test cases
create contests
view contest submissions
trigger similarity checks
view contest analytics
```

Cannot:

```text
change global system settings
manage admins
access unrelated private data unless permitted
```

---

### Admin

Can:

```text
manage users
view audit logs
review plagiarism flags
view platform analytics
manage contests
monitor system health
```

---

### Super Admin

Can:

```text
manage all users
manage admin/faculty roles
view all audit logs
control global configuration
perform system-level operations
```

---

## 6.3 Permission-Based Authorization

In addition to roles, define permissions.

Examples:

```text
PROBLEM_CREATE
PROBLEM_UPDATE
TEST_CASE_CREATE
CONTEST_CREATE
SUBMISSION_CREATE
SUBMISSION_REVIEW
PLAGIARISM_REVIEW
AUDIT_LOG_VIEW
USER_MANAGE
SYSTEM_HEALTH_VIEW
```

Recommended Spring usage:

```java
@PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
```

or later:

```java
@PreAuthorize("hasAuthority('PROBLEM_CREATE')")
```

---

## 7. Hidden Test Case Security

Hidden test cases are among the most sensitive data in the platform.

Rules:

```text
Students must never receive hidden input/output.
Hidden test cases must not appear in frontend responses.
Hidden test cases must not appear in logs.
Only faculty/admin APIs can access hidden tests.
Evaluation worker can access hidden tests internally.
```

Student problem API should return only:

```text
problem statement
constraints
sample input
sample output
```

---

## 8. Submission Security

Submitted code should be treated as sensitive and potentially malicious.

Rules:

```text
Never execute submitted code directly in Spring Boot.
Use Judge0 CE for isolated execution.
Limit source code size.
Store source code hash.
Do not expose full source code in list APIs.
Students can view only their own source code.
Faculty/admin access should be permission-controlled.
```

---

## 9. Code Execution Security

## 9.1 Execution Model

CodeJudgeX uses:

```text
Judge0 CE self-hosted
Docker-based execution
```

Spring Boot should only call Judge0.

It should not compile or run user code directly on the host.

---

## 9.2 Execution Controls

Use execution limits:

```text
time limit
memory limit
output size limit
compile timeout
execution timeout
network restrictions where possible
temporary execution environment
```

---

## 9.3 Security Warning

Important limitation:

```text
Judge0 CE improves isolation, but running untrusted code is always risky.
For commercial hostile-code environments, deeper sandbox hardening is required.
```

Positioning:

```text
CodeJudgeX is designed for educational and self-hosted assessment use.
```

---

## 10. API Security

API security rules:

```text
Validate all request bodies.
Use DTOs, not entities.
Do not expose internal stack traces.
Do not expose password hashes.
Do not expose refresh token hashes.
Do not expose hidden test cases.
Use standard error responses.
Use JWT for protected endpoints.
Apply method-level authorization.
Use pagination for list APIs.
Limit request payload size.
```

---

## 11. Input Validation

Use Jakarta Validation:

```text
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Pattern
```

Validate:

```text
registration payload
login payload
problem creation payload
test case payload
contest creation payload
submission payload
admin operations
```

---

## 12. Rate Limiting

Use Redis for rate limiting.

Rate-limit these actions:

```text
login attempts
refresh token attempts
code submissions
contest join attempts
plagiarism check triggers
admin export operations
```

Example Redis keys:

```text
rate_limit:login:ip:{ipAddress}
rate_limit:submission:user:{userId}:contest:{contestId}
rate_limit:plagiarism:contest:{contestId}
```

HTTP status for rate limit:

```text
429 Too Many Requests
```

---

## 13. Brute Force Protection

Protect login API using:

```text
failed login attempt tracking
IP-based rate limiting
email-based rate limiting
account temporary lock optional
login audit logs
```

Example:

```text
5 failed attempts in 10 minutes → temporary lock or cooldown
```

---

## 14. CORS Policy

Use strict CORS.

Local allowed origins:

```text
http://localhost:5173
```

Do not use:

```text
*
```

for production-like configs.

---

## 15. Secure Headers

Configure security headers through Spring Security or Nginx.

Recommended headers:

```text
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Content-Security-Policy: default-src 'self'
```

---

## 16. Environment and Secret Management

Secrets must not be hardcoded.

Use:

```text
.env files for local development
GitHub Actions secrets for CI
Docker Compose environment variables
```

Sensitive values:

```text
JWT_SECRET
DATABASE_PASSWORD
REDIS_PASSWORD optional
RABBITMQ_PASSWORD
JUDGE0_API_URL
```

Rules:

```text
Do not commit real secrets.
Commit .env.example only.
```

---

## 17. Audit Logging

Audit logs are required for accountability.

Track:

```text
USER_REGISTERED
USER_LOGIN
USER_LOGOUT
ROLE_CHANGED
PROBLEM_CREATED
PROBLEM_UPDATED
TEST_CASE_CREATED
TEST_CASE_UPDATED
CONTEST_CREATED
CONTEST_STARTED
CONTEST_ENDED
SUBMISSION_CREATED
SUBMISSION_EVALUATED
PLAGIARISM_CHECK_STARTED
PLAGIARISM_FLAGGED
ADMIN_REJUDGED_SUBMISSION
```

Audit fields:

```text
actor_id
action
resource_type
resource_id
ip_address
user_agent
request_id
metadata
created_at
```

---

## 18. Logging Security

Do not log:

```text
passwords
JWT tokens
refresh tokens
hidden test case data
full source code by default
secrets
database passwords
```

Safe to log:

```text
request ID
user ID
role
action
submission ID
contest ID
status
error code
latency
```

---

## 19. Database Security

Database security rules:

```text
Use foreign keys.
Use unique constraints.
Use transactions.
Use Flyway migrations.
Do not expose entities directly.
Use least privilege DB user if possible.
Store refresh tokens hashed.
Store source code hash.
Preserve submission immutability.
```

---

## 20. Queue Security

RabbitMQ security rules:

```text
Use credentials, not guest account for app connection.
Use durable queues.
Use manual acknowledgements.
Validate message payloads.
Avoid sensitive data in message body if not needed.
Use DLQ for failed messages.
```

---

## 21. Frontend Security

Frontend rules:

```text
Protect routes by role.
Do not show hidden test cases.
Do not store sensitive secrets in frontend.
Handle token expiry.
Sanitize rendered markdown/problem statements.
Avoid dangerouslySetInnerHTML unless sanitized.
Show proper unauthorized screens.
```

Token storage approach:

```text
MVP: localStorage acceptable for learning project
Better: httpOnly secure cookies
```

Document whichever approach is chosen.

---

## 22. Admin Security

Admin APIs must require strict authorization.

Admin-only actions:

```text
manage users
change roles
view audit logs
review plagiarism flags
trigger rejudge
view system health
export data
```

Admin actions should always create audit logs.

---

## 23. Data Exposure Rules

Never expose:

```text
password_hash
refresh_token_hash
JWT secret
hidden test cases to students
internal stack traces
full audit metadata to unauthorized users
RabbitMQ credentials
DB credentials
```

Restrict exposure of:

```text
student source code
plagiarism flags
admin analytics
system health metrics
```

---

## 24. Security Testing

Test these scenarios:

```text
student cannot create problem
student cannot access hidden test cases
student cannot view another student's private submission
faculty cannot manage admin roles
expired JWT is rejected
invalid JWT is rejected
rate limit works
hidden tests are not returned in APIs
password hash is never exposed
unauthorized admin API access is blocked
```

---

## 25. Security Checklist

Before release, verify:

```text
BCrypt enabled
JWT expiry configured
refresh tokens hashed
CORS restricted
hidden tests protected
role checks applied
DTOs used everywhere
validation enabled
audit logs working
rate limiting enabled
secrets not committed
Swagger restricted if needed
RabbitMQ credentials configured
Judge0 not publicly exposed unnecessarily
```

---

## 26. Future Security Improvements

Future improvements:

```text
Keycloak integration
OAuth2/OpenID Connect
httpOnly cookie auth
2FA for admin users
fine-grained permissions
Nginx reverse proxy
local HTTPS with mkcert
Loki log aggregation
advanced sandbox hardening
security scanning in CI
OWASP dependency check
```

---

## 27. Final Summary

CodeJudgeX security is built around protecting accounts, hidden test cases, submissions, results, and administrative actions.

The most important security rules are:

```text
Never run user code directly in Spring Boot.
Never expose hidden test cases to students.
Never store plain text passwords.
Never skip role checks.
Never ignore audit logging.
Never treat Redis as permanent secure storage.
```

Security must be implemented from the beginning, not added at the end.

