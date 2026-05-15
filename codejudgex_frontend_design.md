# CodeJudgeX Frontend Design

## 1. Purpose

This document defines the frontend design for **CodeJudgeX**.

The frontend should provide a clean, fast, role-based interface for students, faculty, and admins. It should feel like a serious coding assessment platform, not a basic dashboard template.

---

## 2. Frontend Stack

CodeJudgeX frontend uses:

```text
React
Vite
TypeScript
Tailwind CSS
shadcn/ui
React Router
TanStack Query
Axios
React Hook Form
Zod
Recharts
Monaco Editor
```

---

## 3. Frontend Goals

The frontend should support:

```text
role-based dashboards
contest browsing
problem solving
browser code editor
submission flow
leaderboard view
faculty problem management
faculty contest management
admin analytics
audit log viewing
clean loading/error states
responsive layouts
```

---

## 4. Frontend Architecture

Recommended folder structure:

```text
frontend/
├── src/
│   ├── app/
│   ├── routes/
│   ├── components/
│   │   ├── ui/
│   │   ├── layout/
│   │   ├── common/
│   │   └── charts/
│   ├── features/
│   │   ├── auth/
│   │   ├── contests/
│   │   ├── problems/
│   │   ├── submissions/
│   │   ├── leaderboard/
│   │   ├── faculty/
│   │   ├── admin/
│   │   ├── plagiarism/
│   │   └── notifications/
│   ├── hooks/
│   ├── lib/
│   ├── services/
│   ├── schemas/
│   ├── types/
│   └── main.tsx
├── package.json
├── vite.config.ts
├── tailwind.config.ts
└── Dockerfile
```

---

## 5. Design Philosophy

The UI should be:

```text
clean
fast
minimal
professional
role-aware
data-driven
mobile-friendly where possible
focused on developer experience
```

Avoid:

```text
overdesigned animations
unnecessary gradients
complex UI before core flows work
cluttered dashboards
fake analytics
```

---

## 6. Main User Interfaces

## 6.1 Public Pages

```text
Landing page
Login page
Register page
Forgot password optional
```

Landing page should explain:

```text
what CodeJudgeX is
key features
zero-cost self-hosted nature
roles supported
tech overview optional
```

---

## 6.2 Student Pages

```text
/student/dashboard
/contests
/contests/:contestId
/contests/:contestId/problems/:problemId
/submissions/my
/submissions/:submissionId
/leaderboard/:contestId
/notifications
```

Student features:

```text
view contests
join contest
solve problems
submit code
view verdicts
view leaderboard
track history
```

---

## 6.3 Faculty Pages

```text
/faculty/dashboard
/faculty/problems
/faculty/problems/create
/faculty/problems/:problemId/edit
/faculty/problems/:problemId/test-cases
/faculty/contests
/faculty/contests/create
/faculty/contests/:contestId
/faculty/contests/:contestId/submissions
/faculty/contests/:contestId/plagiarism
```

Faculty features:

```text
create problems
manage test cases
create contests
add problems to contests
view submissions
review analytics
trigger plagiarism checks
```

---

## 6.4 Admin Pages

```text
/admin/dashboard
/admin/users
/admin/audit-logs
/admin/plagiarism-flags
/admin/system-health
/admin/settings
```

Admin features:

```text
manage users
view platform metrics
review audit logs
monitor health
review suspicious activity
```

---

## 7. Core Screens

## 7.1 Login Screen

Fields:

```text
email
password
```

Actions:

```text
login
show validation errors
redirect based on role
```

After login:

```text
STUDENT → /student/dashboard
FACULTY → /faculty/dashboard
ADMIN → /admin/dashboard
SUPER_ADMIN → /admin/dashboard
```

---

## 7.2 Student Dashboard

Cards:

```text
Active contests
Upcoming contests
Submissions made
Accepted solutions
Current rank highlights
Recent verdicts
```

Sections:

```text
live contests
recent submissions
recommended problems optional
notifications
```

---

## 7.3 Contest List Page

Show:

```text
contest title
status
start time
end time
number of problems
join button
```

Filters:

```text
UPCOMING
LIVE
ENDED
ARCHIVED
```

---

## 7.4 Contest Detail Page

Show:

```text
contest title
description
start/end time
status
problem list
leaderboard link
submission history link
```

Rules:

```text
students can submit only when contest is LIVE
faculty/admin can manage contest based on permission
```

---

## 7.5 Problem Solving Page

This is the most important frontend screen.

Layout:

```text
left panel: problem statement
right panel: Monaco code editor
bottom/right panel: submission result
```

Problem panel:

```text
title
difficulty
tags
statement
input format
output format
constraints
sample test cases
```

Code editor panel:

```text
language selector
Monaco Editor
run sample optional
submit button
reset code button
```

Result panel:

```text
QUEUED
RUNNING
ACCEPTED
WRONG_ANSWER
COMPILATION_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
PARTIALLY_ACCEPTED
```

---

## 7.6 Submission Result Page

Show:

```text
submission ID
problem
contest
language
status
score
execution time
memory used
submitted at
evaluated at
per-test-case results if allowed
error message if any
```

Student view:

```text
hide hidden test case input/output
show verdict summary
```

Faculty/admin view:

```text
can view detailed test case results
can view source code if permitted
```

---

## 7.7 Leaderboard Page

Show:

```text
rank
student name
total score
solved count
last submission time
```

Use:

```text
TanStack Query for fetching
Recharts optional for score distribution
```

Optional later:

```text
live polling every few seconds during contest
leaderboard freeze
```

---

## 7.8 Faculty Problem Management

Screens:

```text
Problem list
Create problem
Edit problem
Manage test cases
```

Create problem form:

```text
title
description
difficulty
tags
input format
output format
constraints
time limit
memory limit
```

Test case form:

```text
input data
expected output
is sample
weight
```

Important:

```text
hidden test cases should be visually marked and protected
```

---

## 7.9 Faculty Contest Management

Screens:

```text
Contest list
Create contest
Edit contest
Add problems to contest
View contest submissions
View contest analytics
```

Create contest form:

```text
title
description
start time
end time
visibility optional
```

---

## 7.10 Admin Dashboard

Cards:

```text
total users
active contests
total submissions
queued submissions
failed submissions
plagiarism flags
system health
```

Charts:

```text
submissions over time
verdict distribution
contest activity
language usage
```

---

## 8. Role-Based Routing

Use protected routes.

Example route guards:

```text
RequireAuth
RequireRole
RequirePermission later
```

Rules:

```text
unauthenticated users go to login
students cannot access faculty routes
faculty cannot access admin-only routes
admin routes require ADMIN or SUPER_ADMIN
```

---

## 9. API Client Design

Use Axios with a shared API client.

Recommended file:

```text
src/lib/api-client.ts
```

Responsibilities:

```text
set base URL
attach JWT token
handle 401 responses
handle refresh token flow
standardize API errors
```

---

## 10. Data Fetching Strategy

Use:

```text
TanStack Query
```

Use for:

```text
server state
loading states
error states
caching
refetching
pagination
mutations
```

Examples:

```text
useContests()
useContestDetails(contestId)
useProblems()
useSubmission(submissionId)
useLeaderboard(contestId)
```

---

## 11. Form Strategy

Use:

```text
React Hook Form
Zod
```

Use for:

```text
login form
register form
create problem form
create contest form
test case form
submission form
admin user update form
```

Benefits:

```text
client-side validation
type-safe schemas
clean error messages
```

---

## 12. Code Editor Design

Use:

```text
Monaco Editor
```

Features:

```text
syntax highlighting
language selection
theme support
font size control
editor reset
submit code
```

Initial languages:

```text
Java
Python optional later
C++ optional later
```

---

## 13. UI Component System

Use:

```text
shadcn/ui
Tailwind CSS
```

Core components:

```text
Button
Input
Textarea
Select
Dialog
Card
Table
Tabs
Badge
Dropdown
Toast
Skeleton
Alert
```

Custom components:

```text
StatusBadge
RoleBadge
VerdictBadge
ContestStatusBadge
LeaderboardTable
SubmissionTable
ProblemCard
MetricCard
CodeEditor
```

---

## 14. Status and Badge Colors

Use consistent visual language.

Submission statuses:

```text
ACCEPTED → green
WRONG_ANSWER → red
PARTIALLY_ACCEPTED → yellow
QUEUED → gray
RUNNING → blue
COMPILATION_ERROR → orange
RUNTIME_ERROR → red
TIME_LIMIT_EXCEEDED → purple
INTERNAL_ERROR → dark red
```

Contest statuses:

```text
UPCOMING → blue
LIVE → green
ENDED → gray
ARCHIVED → muted
```

---

## 15. Loading and Error States

Every API-driven page should handle:

```text
loading
empty state
error state
success state
refetch state
```

Examples:

```text
No contests available
No submissions yet
Leaderboard will appear after submissions
Unable to fetch data. Try again.
```

---

## 16. Notification UX

Use toast notifications for short feedback:

```text
login success
problem created
contest joined
submission queued
submission evaluated
error occurred
```

Use notification page for persistent notifications:

```text
contest started
submission evaluated
plagiarism flagged
admin announcement
```

---

## 17. Submission Polling Strategy

After code submission:

```text
frontend receives submission ID
redirect to submission result page
poll submission status every 2-3 seconds
stop polling when final status is reached
```

Final statuses:

```text
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

## 18. Frontend Security Rules

Rules:

```text
do not expose hidden test cases
do not store backend secrets
do not show admin routes to students
sanitize rendered problem statements
handle token expiry
hide unauthorized UI actions
never trust frontend-only authorization
```

Important:

```text
Frontend route protection improves UX.
Backend authorization is still mandatory.
```

---

## 19. Responsiveness

Priority:

```text
desktop first
laptop first
basic tablet support
mobile support for viewing, not necessarily coding
```

Reason:

```text
coding editor experience is mainly desktop/laptop focused
```

---

## 20. Accessibility Basics

Implement:

```text
keyboard accessible forms
visible focus states
labels for inputs
proper button states
sufficient contrast
semantic headings
```

---

## 21. Frontend Testing

Use:

```text
React Testing Library
Vitest
Playwright optional
```

Test:

```text
login form
protected route behavior
contest list rendering
problem form validation
code submission flow
leaderboard rendering
role-based navigation
```

---

## 22. Frontend Environment Variables

Use `.env`:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Do not expose secrets in frontend env.

---

## 23. Frontend Docker Setup

Frontend should have its own Dockerfile.

Expected service:

```text
frontend
```

Local URL:

```text
http://localhost:5173
```

Production-like serving can later use:

```text
Nginx
```

---

## 24. Frontend Success Criteria

Frontend design is successful if:

```text
students can solve and submit problems easily
faculty can create contests/problems without confusion
admins can monitor system state
Monaco editor works smoothly
role-based routing works
API errors are displayed clearly
submission status polling works
leaderboard is readable
UI looks clean and professional
```

---

## 25. Final Summary

The CodeJudgeX frontend should be clean, role-based, and workflow-driven.

The most important frontend screen is:

```text
Problem Solving Page with Monaco Editor + Submission Result Flow
```

The frontend should not just look good. It should make the core online judge workflow smooth and believable.

