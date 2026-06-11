# Week 2 — Evaluation Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the resolveUserId blocker, add MockMvc tests, build the full async evaluation pipeline (Judge0→EvaluationWorker→Leaderboard→Notifications), and lay the frontend foundation.

**Architecture:** JWT embeds `userId` UUID as a claim; filter stores it as the principal name so controllers extract real UUIDs. Evaluation is async via RabbitMQ: EvaluationWorker consumes, calls Judge0 CE per test case, scores, persists results, updates Redis leaderboard, and fires notification events. Frontend foundation is type-safe service layer only — no pages yet.

**Tech Stack:** Java 21 · Spring Boot 3.3 · JJWT 0.12.5 · Spring AMQP · RestTemplate · Spring Data Redis · Mockito · MockMvc · Spring Security Test · React 18 · TypeScript · Axios · TanStack Query · Zustand

---

## File Map

### Modified (backend)
- `backend/src/main/java/com/codejudgex/auth/filter/JwtAuthenticationFilter.java` — store `userId` as principal name
- `backend/src/main/java/com/codejudgex/contest/controller/ContestController.java` — extract UUID from principal directly
- `backend/src/main/java/com/codejudgex/problem/controller/ProblemController.java` — same
- `backend/src/main/java/com/codejudgex/submission/controller/SubmissionController.java` — same

### Created (backend — evaluation)
- `backend/src/main/java/com/codejudgex/evaluation/client/Judge0Client.java`
- `backend/src/main/java/com/codejudgex/evaluation/client/Judge0Status.java`
- `backend/src/main/java/com/codejudgex/evaluation/dto/Judge0SubmissionRequest.java`
- `backend/src/main/java/com/codejudgex/evaluation/dto/Judge0SubmissionResponse.java`
- `backend/src/main/java/com/codejudgex/evaluation/service/OutputComparator.java`
- `backend/src/main/java/com/codejudgex/evaluation/service/ScoreCalculator.java`
- `backend/src/main/java/com/codejudgex/evaluation/worker/EvaluationWorker.java`
- `backend/src/main/java/com/codejudgex/submission/repository/SubmissionResultRepository.java`

### Created (backend — leaderboard)
- `backend/src/main/java/com/codejudgex/leaderboard/dto/LeaderboardEntryResponse.java`
- `backend/src/main/java/com/codejudgex/leaderboard/service/LeaderboardService.java`
- `backend/src/main/java/com/codejudgex/leaderboard/controller/LeaderboardController.java`

### Created (backend — notification)
- `backend/src/main/java/com/codejudgex/notification/entity/Notification.java`
- `backend/src/main/java/com/codejudgex/notification/repository/NotificationRepository.java`
- `backend/src/main/java/com/codejudgex/notification/dto/NotificationMessage.java`
- `backend/src/main/java/com/codejudgex/notification/dto/NotificationResponse.java`
- `backend/src/main/java/com/codejudgex/notification/service/EmailService.java`
- `backend/src/main/java/com/codejudgex/notification/worker/NotificationWorker.java`
- `backend/src/main/java/com/codejudgex/notification/controller/NotificationController.java`

### Created (tests)
- `backend/src/test/java/com/codejudgex/auth/controller/AuthControllerTest.java`
- `backend/src/test/java/com/codejudgex/problem/controller/ProblemControllerTest.java`
- `backend/src/test/java/com/codejudgex/contest/controller/ContestControllerTest.java`
- `backend/src/test/java/com/codejudgex/submission/controller/SubmissionControllerTest.java`
- `backend/src/test/java/com/codejudgex/evaluation/service/OutputComparatorTest.java`
- `backend/src/test/java/com/codejudgex/evaluation/service/ScoreCalculatorTest.java`
- `backend/src/test/java/com/codejudgex/evaluation/worker/EvaluationWorkerTest.java`
- `backend/src/test/java/com/codejudgex/leaderboard/service/LeaderboardServiceTest.java`

### Created (frontend)
- `frontend/src/types/auth.types.ts`
- `frontend/src/types/problem.types.ts`
- `frontend/src/types/contest.types.ts`
- `frontend/src/types/submission.types.ts`
- `frontend/src/types/leaderboard.types.ts`
- `frontend/src/types/notification.types.ts`
- `frontend/src/types/api.types.ts`
- `frontend/src/lib/queryClient.ts`
- `frontend/src/stores/auth.store.ts`
- `frontend/src/services/auth.service.ts`
- `frontend/src/services/problem.service.ts`
- `frontend/src/services/contest.service.ts`
- `frontend/src/services/submission.service.ts`
- `frontend/src/services/leaderboard.service.ts`
- `frontend/src/services/notification.service.ts`

---

## Task 1: Fix resolveUserId() — JWT filter stores userId as principal

**Problem:** `JwtAuthenticationFilter` sets `email` as the authentication principal name. Controllers call `UUID.nameUUIDFromBytes(email.getBytes())` — a fake UUID that never matches the real DB row. Fix: store `userId` from JWT claims as principal name. Controllers then call `UUID.fromString(principal.getName())` directly.

**Files:**
- Modify: `backend/src/main/java/com/codejudgex/auth/filter/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/codejudgex/contest/controller/ContestController.java`
- Modify: `backend/src/main/java/com/codejudgex/problem/controller/ProblemController.java`
- Modify: `backend/src/main/java/com/codejudgex/submission/controller/SubmissionController.java`

- [ ] **Step 1.1: Update JwtAuthenticationFilter to use userId as principal name**

The filter currently sets `email` as the principal name (line 65). Change it to extract `userId` from claims and use that as principal. Store email as a detail on the auth token so controllers that need email (like /logout) still work.

Replace the entire `doFilterInternal` body with:

```java
@Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    String token = authHeader.substring(7);

    if (!jwtService.isTokenValid(token)) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        Claims claims = jwtService.extractAllClaims(token);
        String userId = claims.get("userId", String.class);
        String email = claims.getSubject();

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());

            // Principal name = userId UUID string; email stored as credential
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, email, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    } catch (Exception e) {
        log.debug("Could not set user authentication: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
}
```

- [ ] **Step 1.2: Update AuthController to get email from credentials, not principal name**

`AuthController.logout()` and `AuthController.me()` currently use `principal.getName()` as the email. Since principal name is now userId UUID, they must cast to `UsernamePasswordAuthenticationToken` to get the email from `.getCredentials()`.

In `AuthController.java`, update these two methods:

```java
@PostMapping("/logout")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Revoke all refresh tokens for the current user")
public ApiResponse<Void> logout(@AuthenticationPrincipal Principal principal) {
    String email = resolveEmail(principal);
    authService.logoutByEmail(email);
    return ApiResponse.success(null, "Logged out successfully");
}

@GetMapping("/me")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Get current user profile")
public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal Principal principal) {
    String email = resolveEmail(principal);
    return ApiResponse.success(authService.getProfile(email));
}

private String resolveEmail(Principal principal) {
    if (principal instanceof UsernamePasswordAuthenticationToken token) {
        Object credentials = token.getCredentials();
        if (credentials instanceof String email) {
            return email;
        }
    }
    // Fallback: principal name was email (unauthenticated test scenarios)
    return principal.getName();
}
```

Add import: `import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;`

- [ ] **Step 1.3: Update ContestController — remove resolveUserId(), extract UUID from principal**

Replace the `resolveUserId` private method with a one-liner `resolveUserId(Principal p)` that parses the UUID directly, since principal name is now the userId string:

```java
private UUID resolveUserId(Principal principal) {
    return UUID.fromString(principal.getName());
}
```

That's it — no other changes needed in ContestController since all call sites already pass `principal`.

- [ ] **Step 1.4: Update ProblemController same way**

```java
private UUID resolveUserId(Principal principal) {
    return UUID.fromString(principal.getName());
}
```

- [ ] **Step 1.5: Update SubmissionController same way**

```java
private UUID resolveUserId(Principal principal) {
    return UUID.fromString(principal.getName());
}
```

- [ ] **Step 1.6: Compile to verify no regressions**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 1.7: Run existing unit tests**

```bash
cd e:/CodeJudgeX/backend && ./mvnw test -pl . -Dtest="AuthServiceTest,ProblemServiceTest,ContestServiceTest,SubmissionServiceTest" -q
```

Expected: `Tests run: 19, Failures: 0, Errors: 0`

- [ ] **Step 1.8: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/auth/filter/JwtAuthenticationFilter.java backend/src/main/java/com/codejudgex/auth/controller/AuthController.java backend/src/main/java/com/codejudgex/contest/controller/ContestController.java backend/src/main/java/com/codejudgex/problem/controller/ProblemController.java backend/src/main/java/com/codejudgex/submission/controller/SubmissionController.java
git commit -m "fix(auth): store userId UUID as JWT principal, fix resolveUserId() in all controllers"
```

---

## Task 2: MockMvc Controller Tests

**Context:** Tests use `@WebMvcTest` + `@MockBean` for services + `SecurityMockMvcRequestPostProcessors.jwt()` to inject a fake JWT principal. The principal name must be a UUID string (after Task 1 fix). No Spring context needs a real DB — only the controller and security config are loaded.

**Files:**
- Create: `backend/src/test/java/com/codejudgex/auth/controller/AuthControllerTest.java`
- Create: `backend/src/test/java/com/codejudgex/problem/controller/ProblemControllerTest.java`
- Create: `backend/src/test/java/com/codejudgex/contest/controller/ContestControllerTest.java`
- Create: `backend/src/test/java/com/codejudgex/submission/controller/SubmissionControllerTest.java`

- [ ] **Step 2.1: Create AuthControllerTest**

```java
package com.codejudgex.auth.controller;

import com.codejudgex.auth.dto.AuthResponse;
import com.codejudgex.auth.dto.LoginRequest;
import com.codejudgex.auth.dto.RegisterRequest;
import com.codejudgex.auth.service.AuthService;
import com.codejudgex.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;  // required by JwtAuthenticationFilter bean

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .roles(Set.of("STUDENT"))
                .accessToken("sample.jwt.token")
                .accessTokenExpiresIn(900_000L)
                .build();
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("sample.jwt.token"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setPassword("password123");
        // email intentionally missing

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("sample.jwt.token"));
    }
}
```

- [ ] **Step 2.2: Create ProblemControllerTest**

```java
package com.codejudgex.problem.controller;

import com.codejudgex.auth.service.JwtService;
import com.codejudgex.problem.dto.CreateProblemRequest;
import com.codejudgex.problem.dto.ProblemResponse;
import com.codejudgex.problem.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
class ProblemControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProblemService problemService;
    @MockBean JwtService jwtService;

    private static final UUID FACULTY_ID = UUID.randomUUID();

    private ProblemResponse sampleProblem() {
        return ProblemResponse.builder()
                .id(UUID.randomUUID())
                .title("Two Sum")
                .difficulty("EASY")
                .sampleTestCases(List.of())
                .build();
    }

    @Test
    void createProblem_asFaculty_returns201() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setTitle("Two Sum");
        request.setDescription("Find two numbers that sum to target");
        request.setDifficulty("EASY");
        request.setTimeLimitMs(2000);
        request.setMemoryLimitMb(256);

        when(problemService.createProblem(any(), eq(FACULTY_ID))).thenReturn(sampleProblem());

        mockMvc.perform(post("/problems")
                        .with(jwt().jwt(j -> j.subject(FACULTY_ID.toString())
                                .claim("userId", FACULTY_ID.toString())
                                .claim("roles", List.of("FACULTY"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Two Sum"));
    }

    @Test
    void createProblem_asStudent_returns403() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setTitle("Two Sum");
        request.setDescription("desc");
        request.setDifficulty("EASY");
        request.setTimeLimitMs(2000);
        request.setMemoryLimitMb(256);

        mockMvc.perform(post("/problems")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("STUDENT"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProblems_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2.3: Create ContestControllerTest**

```java
package com.codejudgex.contest.controller;

import com.codejudgex.auth.service.JwtService;
import com.codejudgex.contest.dto.ContestResponse;
import com.codejudgex.contest.dto.CreateContestRequest;
import com.codejudgex.contest.service.ContestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContestController.class)
class ContestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ContestService contestService;
    @MockBean JwtService jwtService;

    private static final UUID FACULTY_ID = UUID.randomUUID();

    @Test
    void createContest_asFaculty_returns201() throws Exception {
        CreateContestRequest request = new CreateContestRequest();
        request.setTitle("Spring Contest");
        request.setDescription("A test contest");
        request.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        request.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS));

        ContestResponse response = ContestResponse.builder()
                .id(UUID.randomUUID())
                .title("Spring Contest")
                .status("DRAFT")
                .problems(List.of())
                .build();

        when(contestService.createContest(any(), eq(FACULTY_ID))).thenReturn(response);

        mockMvc.perform(post("/contests")
                        .with(jwt().jwt(j -> j.subject(FACULTY_ID.toString())
                                .claim("userId", FACULTY_ID.toString())
                                .claim("roles", List.of("FACULTY"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Spring Contest"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void createContest_asStudent_returns403() throws Exception {
        CreateContestRequest request = new CreateContestRequest();
        request.setTitle("Spring Contest");
        request.setDescription("desc");
        request.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        request.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS));

        mockMvc.perform(post("/contests")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("STUDENT"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2.4: Create SubmissionControllerTest**

```java
package com.codejudgex.submission.controller;

import com.codejudgex.auth.service.JwtService;
import com.codejudgex.submission.dto.CreateSubmissionRequest;
import com.codejudgex.submission.dto.SubmissionResponse;
import com.codejudgex.submission.dto.SubmissionStatusResponse;
import com.codejudgex.submission.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SubmissionService submissionService;
    @MockBean JwtService jwtService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Test
    void submit_asStudent_returns202() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setContestId(UUID.randomUUID());
        request.setProblemId(UUID.randomUUID());
        request.setLanguageId(62);
        request.setSourceCode("public class Main { public static void main(String[] a) {} }");

        SubmissionResponse response = SubmissionResponse.builder()
                .id(SUBMISSION_ID)
                .status("QUEUED")
                .score(0)
                .submittedAt(Instant.now())
                .build();

        when(submissionService.submit(any(), eq(STUDENT_ID))).thenReturn(response);

        mockMvc.perform(post("/submissions")
                        .with(jwt().jwt(j -> j.subject(STUDENT_ID.toString())
                                .claim("userId", STUDENT_ID.toString())
                                .claim("roles", List.of("STUDENT"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.id").value(SUBMISSION_ID.toString()));
    }

    @Test
    void submit_asFaculty_returns403() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setContestId(UUID.randomUUID());
        request.setProblemId(UUID.randomUUID());
        request.setLanguageId(62);
        request.setSourceCode("code");

        mockMvc.perform(post("/submissions")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("FACULTY"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStatus_authenticated_returns200() throws Exception {
        SubmissionStatusResponse statusResponse = SubmissionStatusResponse.builder()
                .id(SUBMISSION_ID)
                .status("ACCEPTED")
                .score(100)
                .build();

        when(submissionService.getStatus(SUBMISSION_ID)).thenReturn(statusResponse);

        mockMvc.perform(get("/submissions/{id}/status", SUBMISSION_ID)
                        .with(jwt().jwt(j -> j.subject(STUDENT_ID.toString())
                                .claim("roles", List.of("STUDENT")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/submissions/{id}/status", SUBMISSION_ID))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2.5: Run all controller tests**

```bash
cd e:/CodeJudgeX/backend && ./mvnw test -Dtest="AuthControllerTest,ProblemControllerTest,ContestControllerTest,SubmissionControllerTest" -q
```

Expected: `Tests run: 10, Failures: 0, Errors: 0`

- [ ] **Step 2.6: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/test/
git commit -m "test(controllers): add MockMvc tests for Auth, Problem, Contest, Submission controllers"
```

---

## Task 3: Judge0Client + Status Mapping

**Context:** Judge0 CE runs at `http://localhost:2358` (Docker). It takes base64-encoded source + stdin and returns a token. We poll that token until status ID is not 1 (In Queue) or 2 (Processing). Judge0 status IDs map to our internal `SubmissionStatus` strings.

**Files:**
- Create: `backend/src/main/java/com/codejudgex/evaluation/client/Judge0Status.java`
- Create: `backend/src/main/java/com/codejudgex/evaluation/dto/Judge0SubmissionRequest.java`
- Create: `backend/src/main/java/com/codejudgex/evaluation/dto/Judge0SubmissionResponse.java`
- Create: `backend/src/main/java/com/codejudgex/evaluation/client/Judge0Client.java`

- [ ] **Step 3.1: Create Judge0Status enum**

```java
package com.codejudgex.evaluation.client;

public enum Judge0Status {
    IN_QUEUE(1),
    PROCESSING(2),
    ACCEPTED(3),
    WRONG_ANSWER(4),
    TIME_LIMIT_EXCEEDED(5),
    COMPILATION_ERROR(6),
    RUNTIME_ERROR_SIGSEGV(7),
    RUNTIME_ERROR_SIGXFSZ(8),
    RUNTIME_ERROR_SIGFPE(9),
    RUNTIME_ERROR_SIGABRT(10),
    RUNTIME_ERROR_NZEC(11),
    RUNTIME_ERROR_OTHER(12),
    INTERNAL_ERROR(13),
    EXEC_FORMAT_ERROR(14);

    private final int id;

    Judge0Status(int id) { this.id = id; }

    public int getId() { return id; }

    public static Judge0Status fromId(int id) {
        for (Judge0Status s : values()) {
            if (s.id == id) return s;
        }
        return INTERNAL_ERROR;
    }

    public boolean isTerminal() {
        return this != IN_QUEUE && this != PROCESSING;
    }

    /** Map to our internal submission status string */
    public String toSubmissionStatus() {
        return switch (this) {
            case ACCEPTED -> "ACCEPTED";
            case WRONG_ANSWER -> "WRONG_ANSWER";
            case TIME_LIMIT_EXCEEDED -> "TIME_LIMIT_EXCEEDED";
            case COMPILATION_ERROR -> "COMPILATION_ERROR";
            case RUNTIME_ERROR_SIGSEGV, RUNTIME_ERROR_SIGXFSZ,
                 RUNTIME_ERROR_SIGFPE, RUNTIME_ERROR_SIGABRT,
                 RUNTIME_ERROR_NZEC, RUNTIME_ERROR_OTHER -> "RUNTIME_ERROR";
            default -> "INTERNAL_ERROR";
        };
    }
}
```

- [ ] **Step 3.2: Create Judge0SubmissionRequest DTO**

```java
package com.codejudgex.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Judge0SubmissionRequest {

    @JsonProperty("source_code")
    private String sourceCode;          // base64-encoded

    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("stdin")
    private String stdin;               // base64-encoded

    @JsonProperty("cpu_time_limit")
    private double cpuTimeLimit;        // seconds

    @JsonProperty("memory_limit")
    private int memoryLimit;            // kilobytes
}
```

- [ ] **Step 3.3: Create Judge0SubmissionResponse DTO**

```java
package com.codejudgex.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Judge0SubmissionResponse {

    private String token;

    private StatusDto status;

    private String stdout;      // base64-encoded

    private String stderr;      // base64-encoded

    @JsonProperty("compile_output")
    private String compileOutput; // base64-encoded

    private String time;        // seconds as string e.g. "0.123"

    private Integer memory;     // kilobytes

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusDto {
        private int id;
        private String description;
    }
}
```

- [ ] **Step 3.4: Create Judge0Client**

```java
package com.codejudgex.evaluation.client;

import com.codejudgex.evaluation.dto.Judge0SubmissionRequest;
import com.codejudgex.evaluation.dto.Judge0SubmissionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Component
public class Judge0Client {

    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 1_000;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public Judge0Client(
            @Value("${app.judge0.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    /**
     * Submit source code + stdin to Judge0 CE.
     * Returns the submission token for polling.
     */
    public String submitCode(String sourceCode, String stdin, int languageId,
                              int timeLimitMs, int memoryLimitMb) {
        Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                .sourceCode(base64Encode(sourceCode))
                .stdin(base64Encode(stdin != null ? stdin : ""))
                .languageId(languageId)
                .cpuTimeLimit(timeLimitMs / 1000.0)
                .memoryLimit(memoryLimitMb * 1024)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = baseUrl + "/submissions?base64_encoded=true&wait=false";
        Judge0SubmissionResponse response = restTemplate.postForObject(
                url,
                new HttpEntity<>(request, headers),
                Judge0SubmissionResponse.class);

        if (response == null || response.getToken() == null) {
            throw new RuntimeException("Judge0 did not return a submission token");
        }
        return response.getToken();
    }

    /**
     * Poll Judge0 until status is terminal (not In Queue / Processing).
     * Returns the final response.
     */
    public Judge0SubmissionResponse pollResult(String token) {
        String url = baseUrl + "/submissions/" + token + "?base64_encoded=true";

        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            Judge0SubmissionResponse response = restTemplate.getForObject(url, Judge0SubmissionResponse.class);

            if (response == null) {
                throw new RuntimeException("Null response from Judge0 while polling token: " + token);
            }

            int statusId = response.getStatus() != null ? response.getStatus().getId() : 13;
            Judge0Status status = Judge0Status.fromId(statusId);

            if (status.isTerminal()) {
                return response;
            }

            log.debug("Judge0 token {} status: {} (attempt {}/{})", token, status, attempt + 1, MAX_POLL_ATTEMPTS);
            sleep(POLL_INTERVAL_MS);
        }

        throw new RuntimeException("Judge0 evaluation timed out after " + MAX_POLL_ATTEMPTS + " poll attempts for token: " + token);
    }

    /** Decode base64 stdout/stderr from Judge0 response. Returns empty string if null. */
    public String decodeOutput(String base64) {
        if (base64 == null || base64.isBlank()) return "";
        return new String(Base64.getDecoder().decode(base64));
    }

    private String base64Encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

- [ ] **Step 3.5: Compile**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3.6: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/evaluation/
git commit -m "feat(evaluation): add Judge0Client with status mapping and polling"
```

---

## Task 4: OutputComparator + ScoreCalculator

**Files:**
- Create: `backend/src/main/java/com/codejudgex/evaluation/service/OutputComparator.java`
- Create: `backend/src/main/java/com/codejudgex/evaluation/service/ScoreCalculator.java`
- Create: `backend/src/test/java/com/codejudgex/evaluation/service/OutputComparatorTest.java`
- Create: `backend/src/test/java/com/codejudgex/evaluation/service/ScoreCalculatorTest.java`

- [ ] **Step 4.1: Write OutputComparatorTest first (TDD)**

```java
package com.codejudgex.evaluation.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void exactMatch_returnsTrue() {
        assertThat(comparator.matches("hello world", "hello world")).isTrue();
    }

    @Test
    void trailingNewline_returnsTrue() {
        assertThat(comparator.matches("hello world\n", "hello world")).isTrue();
    }

    @Test
    void trailingWhitespace_returnsTrue() {
        assertThat(comparator.matches("hello world   \n", "hello world")).isTrue();
    }

    @Test
    void differentContent_returnsFalse() {
        assertThat(comparator.matches("hello world", "hello earth")).isFalse();
    }

    @Test
    void windowsLineEndings_returnsTrue() {
        assertThat(comparator.matches("line1\r\nline2", "line1\nline2")).isTrue();
    }

    @Test
    void multilineMatch_returnsTrue() {
        assertThat(comparator.matches("1\n2\n3\n", "1\n2\n3")).isTrue();
    }

    @Test
    void nullActual_returnsFalse() {
        assertThat(comparator.matches(null, "expected")).isFalse();
    }

    @Test
    void emptyExpected_emptyActual_returnsTrue() {
        assertThat(comparator.matches("", "")).isTrue();
    }
}
```

- [ ] **Step 4.2: Run test — verify it fails**

```bash
cd e:/CodeJudgeX/backend && ./mvnw test -Dtest="OutputComparatorTest" 2>&1 | tail -5
```

Expected: compilation error (class not found)

- [ ] **Step 4.3: Implement OutputComparator**

```java
package com.codejudgex.evaluation.service;

import org.springframework.stereotype.Component;

@Component
public class OutputComparator {

    /**
     * Compare actual Judge0 stdout against expected output.
     * Normalizes: trims trailing whitespace per line, normalizes CRLF → LF,
     * strips trailing blank lines.
     */
    public boolean matches(String actual, String expected) {
        if (actual == null) return false;
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n")
                   .replace("\r", "\n")
                   .stripTrailing();
    }
}
```

- [ ] **Step 4.4: Write ScoreCalculatorTest**

```java
package com.codejudgex.evaluation.service;

import com.codejudgex.submission.entity.SubmissionResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    private SubmissionResult result(String status, int weight) {
        SubmissionResult r = new SubmissionResult();
        r.setStatus(status);
        r.setWeight(weight);
        return r;
    }

    @Test
    void allAccepted_returnsFullScore() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 2),
                result("ACCEPTED", 3)
        );
        assertThat(calculator.calculate(results)).isEqualTo(5);
    }

    @Test
    void noneAccepted_returnsZero() {
        List<SubmissionResult> results = List.of(
                result("WRONG_ANSWER", 2),
                result("TIME_LIMIT_EXCEEDED", 3)
        );
        assertThat(calculator.calculate(results)).isZero();
    }

    @Test
    void partialAccepted_returnsSumOfAcceptedWeights() {
        List<SubmissionResult> results = List.of(
                result("ACCEPTED", 5),
                result("WRONG_ANSWER", 3),
                result("ACCEPTED", 2)
        );
        assertThat(calculator.calculate(results)).isEqualTo(7);
    }

    @Test
    void emptyList_returnsZero() {
        assertThat(calculator.calculate(List.of())).isZero();
    }
}
```

- [ ] **Step 4.5: Implement ScoreCalculator**

Note: `SubmissionResult` entity needs a `weight` field. Add it now.

First, add `weight` to `SubmissionResult.java`:

```java
// Add this field after errorMessage:
@Column(nullable = false)
private int weight = 1;
```

Then create `ScoreCalculator.java`:

```java
package com.codejudgex.evaluation.service;

import com.codejudgex.submission.entity.SubmissionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreCalculator {

    /**
     * Sum the weights of all ACCEPTED test case results.
     */
    public int calculate(List<SubmissionResult> results) {
        return results.stream()
                .filter(r -> "ACCEPTED".equals(r.getStatus()))
                .mapToInt(SubmissionResult::getWeight)
                .sum();
    }
}
```

- [ ] **Step 4.6: Run tests**

```bash
cd e:/CodeJudgeX/backend && ./mvnw test -Dtest="OutputComparatorTest,ScoreCalculatorTest" -q
```

Expected: `Tests run: 12, Failures: 0, Errors: 0`

- [ ] **Step 4.7: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/evaluation/service/ backend/src/main/java/com/codejudgex/submission/entity/SubmissionResult.java backend/src/test/java/com/codejudgex/evaluation/
git commit -m "feat(evaluation): add OutputComparator and ScoreCalculator with full unit tests"
```

---

## Task 5: SubmissionResultRepository + EvaluationWorker

**Context:** EvaluationWorker is a `@RabbitListener` on `evaluation.queue`. It receives `EvaluationMessage`, calls Judge0 for each test case, builds `SubmissionResult` rows, calculates the final score, updates `Submission`, then publishes a leaderboard update event. On exception: manual NACK to retry queue; after 3 retries DLQ receives it and submission is marked INTERNAL_ERROR.

**Key:** `application.yml` has `acknowledge-mode: manual`, so the listener must use `Channel.basicAck` / `Channel.basicNack`.

**Files:**
- Create: `backend/src/main/java/com/codejudgex/submission/repository/SubmissionResultRepository.java`
- Create: `backend/src/main/java/com/codejudgex/evaluation/worker/EvaluationWorker.java`
- Create: `backend/src/test/java/com/codejudgex/evaluation/worker/EvaluationWorkerTest.java`

- [ ] **Step 5.1: Create SubmissionResultRepository**

```java
package com.codejudgex.submission.repository;

import com.codejudgex.submission.entity.SubmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionResultRepository extends JpaRepository<SubmissionResult, UUID> {

    List<SubmissionResult> findBySubmissionId(UUID submissionId);
}
```

- [ ] **Step 5.2: Create EvaluationWorker**

```java
package com.codejudgex.evaluation.worker;

import com.codejudgex.evaluation.client.Judge0Client;
import com.codejudgex.evaluation.client.Judge0Status;
import com.codejudgex.evaluation.dto.Judge0SubmissionResponse;
import com.codejudgex.evaluation.service.OutputComparator;
import com.codejudgex.evaluation.service.ScoreCalculator;
import com.codejudgex.infrastructure.config.RabbitMQConfig;
import com.codejudgex.problem.entity.TestCase;
import com.codejudgex.problem.repository.TestCaseRepository;
import com.codejudgex.submission.dto.EvaluationMessage;
import com.codejudgex.submission.entity.Submission;
import com.codejudgex.submission.entity.SubmissionResult;
import com.codejudgex.submission.repository.SubmissionRepository;
import com.codejudgex.submission.repository.SubmissionResultRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationWorker {

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final TestCaseRepository testCaseRepository;
    private final Judge0Client judge0Client;
    private final OutputComparator outputComparator;
    private final ScoreCalculator scoreCalculator;

    @RabbitListener(queues = RabbitMQConfig.EVALUATION_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void evaluate(
            EvaluationMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Evaluating submission {} for problem {} in contest {}",
                message.getSubmissionId(), message.getProblemId(), message.getContestId());

        Submission submission = submissionRepository.findById(message.getSubmissionId())
                .orElse(null);

        if (submission == null) {
            log.error("Submission {} not found — discarding message", message.getSubmissionId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        // Idempotency guard: skip if already evaluated
        if (!"QUEUED".equals(submission.getStatus()) && !"RUNNING".equals(submission.getStatus())) {
            log.warn("Submission {} already in terminal state {} — skipping", submission.getId(), submission.getStatus());
            channel.basicAck(deliveryTag, false);
            return;
        }

        submission.setStatus("RUNNING");
        submissionRepository.save(submission);

        try {
            List<TestCase> testCases = testCaseRepository.findByProblemId(message.getProblemId());

            if (testCases.isEmpty()) {
                log.warn("No test cases found for problem {} — marking INTERNAL_ERROR", message.getProblemId());
                markTerminal(submission, "INTERNAL_ERROR", 0);
                channel.basicAck(deliveryTag, false);
                return;
            }

            List<SubmissionResult> results = evaluateAllTestCases(submission, testCases, message);
            submissionResultRepository.saveAll(results);

            int score = scoreCalculator.calculate(results);
            String finalStatus = determineFinalStatus(results);

            markTerminal(submission, finalStatus, score);
            channel.basicAck(deliveryTag, false);

            log.info("Submission {} evaluated: status={} score={}", submission.getId(), finalStatus, score);

        } catch (Exception e) {
            log.error("Evaluation failed for submission {}: {}", message.getSubmissionId(), e.getMessage(), e);
            // NACK without re-queue — message goes to DLQ via dead-letter config
            // Spring AMQP retry will attempt up to max-attempts before DLQ
            channel.basicNack(deliveryTag, false, false);
            markTerminalIfDead(submission);
        }
    }

    private List<SubmissionResult> evaluateAllTestCases(
            Submission submission, List<TestCase> testCases, EvaluationMessage message) {

        List<SubmissionResult> results = new ArrayList<>();

        for (TestCase tc : testCases) {
            SubmissionResult result = evaluateSingleTestCase(submission, tc, message);
            results.add(result);
        }

        return results;
    }

    private SubmissionResult evaluateSingleTestCase(
            Submission submission, TestCase tc, EvaluationMessage message) {

        SubmissionResult result = new SubmissionResult();
        result.setSubmission(submission);
        result.setTestCaseId(tc.getId());
        result.setWeight(tc.getWeight());

        try {
            String token = judge0Client.submitCode(
                    submission.getSourceCode(),
                    tc.getInputData(),
                    submission.getLanguageId(),
                    message.getTimeLimitMs(),
                    message.getMemoryLimitMb());

            Judge0SubmissionResponse judge0Response = judge0Client.pollResult(token);
            Judge0Status judge0Status = Judge0Status.fromId(
                    judge0Response.getStatus() != null ? judge0Response.getStatus().getId() : 13);

            result.setStatus(judge0Status.toSubmissionStatus());

            String actualOutput = judge0Client.decodeOutput(judge0Response.getStdout());
            result.setActualOutput(actualOutput);

            if (judge0Response.getTime() != null) {
                result.setExecutionTimeMs((int) (Double.parseDouble(judge0Response.getTime()) * 1000));
            }

            // For ACCEPTED status: verify output actually matches
            if ("ACCEPTED".equals(result.getStatus())) {
                boolean outputMatches = outputComparator.matches(actualOutput, tc.getExpectedOutput());
                if (!outputMatches) {
                    result.setStatus("WRONG_ANSWER");
                }
            }

            // Attach error info for non-accepted results
            if (!"ACCEPTED".equals(result.getStatus())) {
                String errorInfo = judge0client.decodeOutput(judge0Response.getCompileOutput());
                if (errorInfo.isBlank()) {
                    errorInfo = judge0Client.decodeOutput(judge0Response.getStderr());
                }
                result.setErrorMessage(errorInfo.isBlank() ? null : errorInfo);
            }

        } catch (Exception e) {
            log.error("Judge0 call failed for test case {}: {}", tc.getId(), e.getMessage());
            result.setStatus("INTERNAL_ERROR");
            result.setErrorMessage("Evaluation service error: " + e.getMessage());
        }

        return result;
    }

    private String determineFinalStatus(List<SubmissionResult> results) {
        // If any result is COMPILATION_ERROR, the whole submission is CE
        boolean hasCompilationError = results.stream()
                .anyMatch(r -> "COMPILATION_ERROR".equals(r.getStatus()));
        if (hasCompilationError) return "COMPILATION_ERROR";

        boolean allAccepted = results.stream()
                .allMatch(r -> "ACCEPTED".equals(r.getStatus()));
        if (allAccepted) return "ACCEPTED";

        // Check for specific error types
        for (SubmissionResult r : results) {
            if ("TIME_LIMIT_EXCEEDED".equals(r.getStatus())) return "TIME_LIMIT_EXCEEDED";
            if ("RUNTIME_ERROR".equals(r.getStatus())) return "RUNTIME_ERROR";
            if ("MEMORY_LIMIT_EXCEEDED".equals(r.getStatus())) return "MEMORY_LIMIT_EXCEEDED";
        }

        boolean hasPartial = results.stream().anyMatch(r -> "ACCEPTED".equals(r.getStatus()));
        return hasPartial ? "PARTIALLY_ACCEPTED" : "WRONG_ANSWER";
    }

    private void markTerminal(Submission submission, String status, int score) {
        submission.setStatus(status);
        submission.setScore(score);
        submission.setEvaluatedAt(Instant.now());
        submissionRepository.save(submission);
    }

    private void markTerminalIfDead(Submission submission) {
        if ("RUNNING".equals(submission.getStatus())) {
            submission.setStatus("INTERNAL_ERROR");
            submission.setEvaluatedAt(Instant.now());
            submissionRepository.save(submission);
        }
    }
}
```

> **Note:** There is a typo `judge0client` (lowercase c) on the compile_output decode line — fix to `judge0Client`.

- [ ] **Step 5.3: Fix typo in EvaluationWorker**

In `EvaluationWorker.java` find:
```java
String errorInfo = judge0client.decodeOutput(judge0Response.getCompileOutput());
```
Replace with:
```java
String errorInfo = judge0Client.decodeOutput(judge0Response.getCompileOutput());
```

- [ ] **Step 5.4: Write EvaluationWorkerTest**

```java
package com.codejudgex.evaluation.worker;

import com.codejudgex.evaluation.client.Judge0Client;
import com.codejudgex.evaluation.dto.Judge0SubmissionResponse;
import com.codejudgex.evaluation.service.OutputComparator;
import com.codejudgex.evaluation.service.ScoreCalculator;
import com.codejudgex.problem.entity.TestCase;
import com.codejudgex.problem.repository.TestCaseRepository;
import com.codejudgex.submission.dto.EvaluationMessage;
import com.codejudgex.submission.entity.Submission;
import com.codejudgex.submission.entity.SubmissionResult;
import com.codejudgex.submission.repository.SubmissionRepository;
import com.codejudgex.submission.repository.SubmissionResultRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationWorkerTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionResultRepository submissionResultRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock Judge0Client judge0Client;
    @Mock OutputComparator outputComparator;
    @Mock ScoreCalculator scoreCalculator;
    @Mock Channel channel;

    @InjectMocks EvaluationWorker worker;

    private UUID submissionId;
    private UUID problemId;
    private Submission submission;
    private EvaluationMessage message;
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        problemId = UUID.randomUUID();

        submission = new Submission();
        submission.setStatus("QUEUED");
        submission.setSourceCode("public class Main {}");
        submission.setLanguageId(62);

        message = EvaluationMessage.builder()
                .submissionId(submissionId)
                .problemId(problemId)
                .contestId(UUID.randomUUID())
                .languageId(62)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .build();

        testCase = new TestCase();
        testCase.setInputData("5");
        testCase.setExpectedOutput("25");
        testCase.setWeight(1);
    }

    @Test
    void evaluate_allAccepted_marksAccepted() throws IOException {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenReturn(submission);
        when(testCaseRepository.findByProblemId(problemId)).thenReturn(List.of(testCase));

        Judge0SubmissionResponse judge0Response = buildJudge0Response(3, "25\n");
        when(judge0Client.submitCode(anyString(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn("token-abc");
        when(judge0Client.pollResult("token-abc")).thenReturn(judge0Response);
        when(judge0Client.decodeOutput("MjUK")).thenReturn("25\n");
        when(judge0Client.decodeOutput(null)).thenReturn("");
        when(outputComparator.matches("25\n", "25")).thenReturn(true);
        when(scoreCalculator.calculate(any())).thenReturn(1);
        when(submissionResultRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        worker.evaluate(message, channel, 1L);

        verify(channel).basicAck(1L, false);
        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository, atLeast(2)).save(captor.capture());
        List<Submission> saves = captor.getAllValues();
        Submission final_ = saves.get(saves.size() - 1);
        assertThat(final_.getStatus()).isEqualTo("ACCEPTED");
        assertThat(final_.getScore()).isEqualTo(1);
    }

    @Test
    void evaluate_submissionNotFound_acksAndSkips() throws IOException {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        worker.evaluate(message, channel, 2L);

        verify(channel).basicAck(2L, false);
        verify(testCaseRepository, never()).findByProblemId(any());
    }

    @Test
    void evaluate_alreadyTerminal_acksAndSkips() throws IOException {
        submission.setStatus("ACCEPTED");
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        worker.evaluate(message, channel, 3L);

        verify(channel).basicAck(3L, false);
        verify(judge0Client, never()).submitCode(any(), any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void evaluate_judge0Fails_nacksAndMarksInternalError() throws IOException {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenReturn(submission);
        when(testCaseRepository.findByProblemId(problemId)).thenReturn(List.of(testCase));
        when(judge0Client.submitCode(anyString(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Judge0 unavailable"));

        worker.evaluate(message, channel, 4L);

        verify(channel).basicNack(4L, false, false);
    }

    private Judge0SubmissionResponse buildJudge0Response(int statusId, String stdoutBase64) {
        Judge0SubmissionResponse r = new Judge0SubmissionResponse();
        Judge0SubmissionResponse.StatusDto status = new Judge0SubmissionResponse.StatusDto();
        status.setId(statusId);
        r.setStatus(status);
        r.setStdout(stdoutBase64);
        r.setTime("0.123");
        return r;
    }
}
```

- [ ] **Step 5.5: Compile and run all tests**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q && ./mvnw test -q
```

Expected: All tests pass (19 existing + 12 new = 31+ passing)

- [ ] **Step 5.6: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/evaluation/ backend/src/main/java/com/codejudgex/submission/repository/SubmissionResultRepository.java backend/src/test/java/com/codejudgex/evaluation/worker/
git commit -m "feat(evaluation): add EvaluationWorker with Judge0 integration, retry, and DLQ handling"
```

---

## Task 6: LeaderboardService + LeaderboardController

**Context:** Leaderboard uses Redis sorted sets. Key: `leaderboard:contest:{contestId}`. Member = `studentId` string. Score = `totalScore` integer. Falls back to PostgreSQL `leaderboard_snapshots` if Redis is empty.

**Files:**
- Create: `backend/src/main/java/com/codejudgex/leaderboard/dto/LeaderboardEntryResponse.java`
- Create: `backend/src/main/java/com/codejudgex/leaderboard/service/LeaderboardService.java`
- Create: `backend/src/main/java/com/codejudgex/leaderboard/controller/LeaderboardController.java`
- Create: `backend/src/test/java/com/codejudgex/leaderboard/service/LeaderboardServiceTest.java`

- [ ] **Step 6.1: Create LeaderboardEntryResponse DTO**

```java
package com.codejudgex.leaderboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LeaderboardEntryResponse {
    private int rank;
    private UUID studentId;
    private String studentName;
    private int totalScore;
    private int solvedCount;
}
```

- [ ] **Step 6.2: Create LeaderboardService**

```java
package com.codejudgex.leaderboard.service;

import com.codejudgex.leaderboard.dto.LeaderboardEntryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final String KEY_PREFIX = "leaderboard:contest:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Add or update a student's score in the leaderboard sorted set.
     * Uses ZADD with the new score (overwrites previous).
     */
    public void updateScore(UUID contestId, UUID studentId, int score) {
        String key = leaderboardKey(contestId);
        redisTemplate.opsForZSet().add(key, studentId.toString(), score);
        log.debug("Leaderboard updated: contest={} student={} score={}", contestId, studentId, score);
    }

    /**
     * Return top N entries ranked highest score first (ZREVRANGE with scores).
     */
    public List<LeaderboardEntryResponse> getTopN(UUID contestId, int n) {
        String key = leaderboardKey(contestId);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, n - 1L);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            UUID studentId = UUID.fromString(tuple.getValue());
            int score = tuple.getScore() != null ? tuple.getScore().intValue() : 0;
            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .studentId(studentId)
                    .totalScore(score)
                    .build());
        }
        return result;
    }

    /**
     * Return a specific student's rank (1-based). Returns -1 if not on board.
     */
    public long getStudentRank(UUID contestId, UUID studentId) {
        String key = leaderboardKey(contestId);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, studentId.toString());
        return rank == null ? -1L : rank + 1L; // 0-based → 1-based
    }

    /**
     * Return a specific student's score. Returns 0 if not on board.
     */
    public int getStudentScore(UUID contestId, UUID studentId) {
        String key = leaderboardKey(contestId);
        Double score = redisTemplate.opsForZSet().score(key, studentId.toString());
        return score == null ? 0 : score.intValue();
    }

    private String leaderboardKey(UUID contestId) {
        return KEY_PREFIX + contestId;
    }
}
```

- [ ] **Step 6.3: Wire LeaderboardService into EvaluationWorker**

Add `LeaderboardService` as a dependency in `EvaluationWorker`. After `markTerminal()` succeeds, call:

```java
// After markTerminal(submission, finalStatus, score);
leaderboardService.updateScore(message.getContestId(), submission.getStudentId(), score);
```

Add field injection in EvaluationWorker:
```java
private final LeaderboardService leaderboardService;
```

The class uses `@RequiredArgsConstructor` so Lombok injects it automatically.

- [ ] **Step 6.4: Create LeaderboardController**

```java
package com.codejudgex.leaderboard.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.leaderboard.dto.LeaderboardEntryResponse;
import com.codejudgex.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Real-time contest rankings")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/contests/{contestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get top 50 leaderboard entries for a contest")
    public ApiResponse<List<LeaderboardEntryResponse>> getLeaderboard(
            @PathVariable UUID contestId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(leaderboardService.getTopN(contestId, Math.min(limit, 200)));
    }

    @GetMapping("/contests/{contestId}/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current student's rank and score in a contest")
    public ApiResponse<LeaderboardEntryResponse> getMyRank(
            @PathVariable UUID contestId,
            @AuthenticationPrincipal Principal principal) {
        UUID studentId = UUID.fromString(principal.getName());
        long rank = leaderboardService.getStudentRank(contestId, studentId);
        int score = leaderboardService.getStudentScore(contestId, studentId);
        return ApiResponse.success(LeaderboardEntryResponse.builder()
                .rank((int) rank)
                .studentId(studentId)
                .totalScore(score)
                .build());
    }
}
```

- [ ] **Step 6.5: Write LeaderboardServiceTest**

```java
package com.codejudgex.leaderboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @InjectMocks LeaderboardService leaderboardService;

    private UUID contestId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        contestId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @Test
    void updateScore_callsZAdd() {
        leaderboardService.updateScore(contestId, studentId, 75);
        verify(zSetOps).add("leaderboard:contest:" + contestId, studentId.toString(), 75.0);
    }

    @Test
    void getTopN_emptyRedis_returnsEmptyList() {
        when(zSetOps.reverseRangeWithScores(anyString(), eq(0L), eq(49L))).thenReturn(Set.of());
        assertThat(leaderboardService.getTopN(contestId, 50)).isEmpty();
    }

    @Test
    void getStudentRank_notOnBoard_returnsMinusOne() {
        when(zSetOps.reverseRank(anyString(), anyString())).thenReturn(null);
        assertThat(leaderboardService.getStudentRank(contestId, studentId)).isEqualTo(-1L);
    }

    @Test
    void getStudentRank_onBoard_returnsOneBased() {
        when(zSetOps.reverseRank(anyString(), anyString())).thenReturn(0L); // 0-based top
        assertThat(leaderboardService.getStudentRank(contestId, studentId)).isEqualTo(1L);
    }

    @Test
    void getStudentScore_notOnBoard_returnsZero() {
        when(zSetOps.score(anyString(), anyString())).thenReturn(null);
        assertThat(leaderboardService.getStudentScore(contestId, studentId)).isZero();
    }
}
```

- [ ] **Step 6.6: Compile + test**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q && ./mvnw test -Dtest="LeaderboardServiceTest" -q
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6.7: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/leaderboard/ backend/src/test/java/com/codejudgex/leaderboard/
git commit -m "feat(leaderboard): add Redis sorted-set leaderboard service and controller"
```

---

## Task 7: Notification Module

**Files:**
- Create: `backend/src/main/java/com/codejudgex/notification/entity/Notification.java`
- Create: `backend/src/main/java/com/codejudgex/notification/repository/NotificationRepository.java`
- Create: `backend/src/main/java/com/codejudgex/notification/dto/NotificationMessage.java`
- Create: `backend/src/main/java/com/codejudgex/notification/dto/NotificationResponse.java`
- Create: `backend/src/main/java/com/codejudgex/notification/service/EmailService.java`
- Create: `backend/src/main/java/com/codejudgex/notification/worker/NotificationWorker.java`
- Create: `backend/src/main/java/com/codejudgex/notification/controller/NotificationController.java`

- [ ] **Step 7.1: Create Notification entity**

```java
package com.codejudgex.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
```

- [ ] **Step 7.2: Create NotificationRepository**

```java
package com.codejudgex.notification.repository;

import com.codejudgex.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(UUID id, UUID userId);
}
```

- [ ] **Step 7.3: Create NotificationMessage POJO**

```java
package com.codejudgex.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private UUID userId;
    private String title;
    private String message;
    private String userEmail;   // for email delivery
}
```

- [ ] **Step 7.4: Create NotificationResponse DTO**

```java
package com.codejudgex.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private boolean isRead;
    private Instant createdAt;
}
```

- [ ] **Step 7.5: Create EmailService**

```java
package com.codejudgex.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@codejudgex.local");
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // Email failure is non-fatal — log and continue
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
```

- [ ] **Step 7.6: Create NotificationWorker**

```java
package com.codejudgex.notification.worker;

import com.codejudgex.infrastructure.config.RabbitMQConfig;
import com.codejudgex.notification.dto.NotificationMessage;
import com.codejudgex.notification.entity.Notification;
import com.codejudgex.notification.repository.NotificationRepository;
import com.codejudgex.notification.service.EmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handle(
            NotificationMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Processing notification for user {}: {}", message.getUserId(), message.getTitle());

        try {
            Notification notification = new Notification();
            notification.setUserId(message.getUserId());
            notification.setTitle(message.getTitle());
            notification.setMessage(message.getMessage());
            notificationRepository.save(notification);

            if (message.getUserEmail() != null && !message.getUserEmail().isBlank()) {
                emailService.sendSimpleEmail(
                        message.getUserEmail(),
                        message.getTitle(),
                        message.getMessage());
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Notification processing failed: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

- [ ] **Step 7.7: Create NotificationController**

```java
package com.codejudgex.notification.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.notification.dto.NotificationResponse;
import com.codejudgex.notification.entity.Notification;
import com.codejudgex.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notifications for current user (paginated)")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(principal.getName());
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        Page<NotificationResponse> mapped = notifications.map(this::toResponse);
        return ApiResponse.success(PageResponse.of(mapped));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<Void> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        notificationRepository.markAsRead(id, userId);
        return ApiResponse.success(null, "Marked as read");
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 7.8: Add PageResponse.of() factory if missing**

Check `PageResponse.java`. If it doesn't have a static `of(Page<T> page)` factory, add:

```java
public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
    );
}
```

- [ ] **Step 7.9: Compile + full test suite**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q && ./mvnw test -q
```

Expected: All tests pass.

- [ ] **Step 7.10: Commit**

```bash
cd e:/CodeJudgeX && git add backend/src/main/java/com/codejudgex/notification/
git commit -m "feat(notification): add NotificationWorker, EmailService, NotificationController"
```

---

## Task 8: Frontend Foundation

**Context:** Pure TypeScript/service layer. No pages yet. This unblocks Week 4.

**Files:** All in `frontend/src/`

- [ ] **Step 8.1: Create shared API type**

Create `frontend/src/types/api.types.ts`:

```typescript
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
```

- [ ] **Step 8.2: Create auth types**

Create `frontend/src/types/auth.types.ts`:

```typescript
export type UserRole = 'STUDENT' | 'FACULTY' | 'ADMIN' | 'SUPER_ADMIN'

export interface AuthUser {
  id: string
  name: string
  email: string
  roles: UserRole[]
}

export interface AuthResponse {
  id: string
  name: string
  email: string
  roles: UserRole[]
  accessToken: string
  accessTokenExpiresIn: number
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  department?: string
  year?: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UserProfileResponse {
  id: string
  name: string
  email: string
  department?: string
  year?: number
  status: string
  roles: UserRole[]
  createdAt: string
}
```

- [ ] **Step 8.3: Create submission types**

Create `frontend/src/types/submission.types.ts`:

```typescript
export type SubmissionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'PARTIALLY_ACCEPTED'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR'
  | 'INTERNAL_ERROR'

export const TERMINAL_STATUSES = new Set<SubmissionStatus>([
  'ACCEPTED',
  'WRONG_ANSWER',
  'PARTIALLY_ACCEPTED',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILATION_ERROR',
  'INTERNAL_ERROR',
])

export interface CreateSubmissionRequest {
  contestId: string
  problemId: string
  languageId: number
  sourceCode: string
}

export interface SubmissionResponse {
  id: string
  status: SubmissionStatus
  score: number
  languageId: number
  submittedAt: string
  evaluatedAt?: string
}

export interface SubmissionStatusResponse {
  id: string
  status: SubmissionStatus
  score: number
}
```

- [ ] **Step 8.4: Create problem + contest + leaderboard + notification types**

Create `frontend/src/types/problem.types.ts`:

```typescript
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface TestCaseResponse {
  id: string
  isSample: boolean
  weight: number
}

export interface ProblemSummaryResponse {
  id: string
  title: string
  difficulty: Difficulty
  timeLimitMs: number
  memoryLimitMb: number
}

export interface ProblemResponse extends ProblemSummaryResponse {
  description: string
  inputFormat?: string
  outputFormat?: string
  constraintsText?: string
  sampleTestCases: TestCaseResponse[]
}

export interface CreateProblemRequest {
  title: string
  description: string
  difficulty: Difficulty
  inputFormat?: string
  outputFormat?: string
  constraintsText?: string
  timeLimitMs: number
  memoryLimitMb: number
}
```

Create `frontend/src/types/contest.types.ts`:

```typescript
export type ContestStatus = 'DRAFT' | 'UPCOMING' | 'LIVE' | 'ENDED'

export interface ContestSummaryResponse {
  id: string
  title: string
  status: ContestStatus
  startTime: string
  endTime: string
  participantCount: number
}

export interface ContestResponse extends ContestSummaryResponse {
  description: string
  problems: Array<{ id: string; title: string; order: number }>
}

export interface CreateContestRequest {
  title: string
  description: string
  startTime: string
  endTime: string
}
```

Create `frontend/src/types/leaderboard.types.ts`:

```typescript
export interface LeaderboardEntryResponse {
  rank: number
  studentId: string
  studentName?: string
  totalScore: number
  solvedCount: number
}
```

Create `frontend/src/types/notification.types.ts`:

```typescript
export interface NotificationResponse {
  id: string
  title: string
  message: string
  isRead: boolean
  createdAt: string
}
```

- [ ] **Step 8.5: Update axios instance**

Replace `frontend/src/lib/axios.ts` content:

```typescript
import axios from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — attach Bearer token
apiClient.interceptors.request.use((config) => {
  // Import lazily to avoid circular dep with store
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor — on 401 redirect to /login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

- [ ] **Step 8.6: Create QueryClient**

Create `frontend/src/lib/queryClient.ts`:

```typescript
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})
```

- [ ] **Step 8.7: Create Zustand auth store**

Create `frontend/src/stores/auth.store.ts`:

```typescript
import { create } from 'zustand'
import type { AuthUser, UserRole } from '@/types/auth.types'

interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  setAuth: (user: AuthUser, token: string) => void
  clearAuth: () => void
  hasRole: (role: UserRole) => boolean
}

const ROLE_HIERARCHY: Record<UserRole, number> = {
  STUDENT: 1,
  FACULTY: 2,
  ADMIN: 3,
  SUPER_ADMIN: 4,
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,

  setAuth: (user, accessToken) => {
    localStorage.setItem('accessToken', accessToken)
    set({ user, accessToken })
  },

  clearAuth: () => {
    localStorage.removeItem('accessToken')
    set({ user: null, accessToken: null })
  },

  hasRole: (role: UserRole) => {
    const { user } = get()
    if (!user) return false
    const required = ROLE_HIERARCHY[role]
    return user.roles.some((r) => ROLE_HIERARCHY[r] >= required)
  },
}))
```

- [ ] **Step 8.8: Create service layer**

Create `frontend/src/services/auth.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { AuthResponse, LoginRequest, RegisterRequest, UserProfileResponse } from '@/types/auth.types'

export const authService = {
  register: (data: RegisterRequest): Promise<AuthResponse> =>
    apiClient.post('/auth/register', data).then((r) => r.data.data),

  login: (data: LoginRequest): Promise<AuthResponse> =>
    apiClient.post('/auth/login', data).then((r) => r.data.data),

  refresh: (refreshToken: string): Promise<AuthResponse> =>
    apiClient.post('/auth/refresh', { refreshToken }).then((r) => r.data.data),

  logout: (): Promise<void> =>
    apiClient.post('/auth/logout').then(() => undefined),

  me: (): Promise<UserProfileResponse> =>
    apiClient.get('/auth/me').then((r) => r.data.data),
}
```

Create `frontend/src/services/submission.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { CreateSubmissionRequest, SubmissionResponse, SubmissionStatusResponse } from '@/types/submission.types'

export const submissionService = {
  submit: (data: CreateSubmissionRequest): Promise<SubmissionResponse> =>
    apiClient.post('/submissions', data).then((r) => r.data.data),

  getStatus: (id: string): Promise<SubmissionStatusResponse> =>
    apiClient.get(`/submissions/${id}/status`).then((r) => r.data.data),

  getSubmission: (id: string): Promise<SubmissionResponse> =>
    apiClient.get(`/submissions/${id}`).then((r) => r.data.data),
}
```

Create `frontend/src/services/problem.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { CreateProblemRequest, ProblemResponse, ProblemSummaryResponse } from '@/types/problem.types'
import type { PageResponse } from '@/types/api.types'

export const problemService = {
  list: (page = 0, size = 20): Promise<PageResponse<ProblemSummaryResponse>> =>
    apiClient.get('/problems', { params: { page, size } }).then((r) => r.data.data),

  get: (id: string): Promise<ProblemResponse> =>
    apiClient.get(`/problems/${id}`).then((r) => r.data.data),

  getFull: (id: string): Promise<ProblemResponse> =>
    apiClient.get(`/problems/${id}/full`).then((r) => r.data.data),

  create: (data: CreateProblemRequest): Promise<ProblemResponse> =>
    apiClient.post('/problems', data).then((r) => r.data.data),
}
```

Create `frontend/src/services/contest.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { ContestResponse, ContestSummaryResponse, CreateContestRequest } from '@/types/contest.types'
import type { PageResponse } from '@/types/api.types'

export const contestService = {
  list: (status?: string, page = 0, size = 20): Promise<PageResponse<ContestSummaryResponse>> =>
    apiClient.get('/contests', { params: { status, page, size } }).then((r) => r.data.data),

  get: (id: string): Promise<ContestResponse> =>
    apiClient.get(`/contests/${id}`).then((r) => r.data.data),

  create: (data: CreateContestRequest): Promise<ContestResponse> =>
    apiClient.post('/contests', data).then((r) => r.data.data),

  register: (id: string): Promise<void> =>
    apiClient.post(`/contests/${id}/register`).then(() => undefined),

  addProblem: (contestId: string, problemId: string): Promise<ContestResponse> =>
    apiClient.post(`/contests/${contestId}/problems`, null, { params: { problemId } }).then((r) => r.data.data),
}
```

Create `frontend/src/services/leaderboard.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { LeaderboardEntryResponse } from '@/types/leaderboard.types'

export const leaderboardService = {
  getTop: (contestId: string, limit = 50): Promise<LeaderboardEntryResponse[]> =>
    apiClient.get(`/leaderboards/contests/${contestId}`, { params: { limit } }).then((r) => r.data.data),

  getMyRank: (contestId: string): Promise<LeaderboardEntryResponse> =>
    apiClient.get(`/leaderboards/contests/${contestId}/me`).then((r) => r.data.data),
}
```

Create `frontend/src/services/notification.service.ts`:

```typescript
import { apiClient } from '@/lib/axios'
import type { NotificationResponse } from '@/types/notification.types'
import type { PageResponse } from '@/types/api.types'

export const notificationService = {
  list: (page = 0, size = 20): Promise<PageResponse<NotificationResponse>> =>
    apiClient.get('/notifications', { params: { page, size } }).then((r) => r.data.data),

  markRead: (id: string): Promise<void> =>
    apiClient.patch(`/notifications/${id}/read`).then(() => undefined),
}
```

- [ ] **Step 8.9: Install frontend deps + typecheck**

```bash
cd e:/CodeJudgeX/frontend && npm install @tanstack/react-query zustand 2>&1 | tail -3
npm run typecheck 2>&1 | tail -10
```

Expected: No type errors.

- [ ] **Step 8.10: Commit**

```bash
cd e:/CodeJudgeX && git add frontend/src/
git commit -m "feat(frontend): add TypeScript types, axios client, QueryClient, Zustand auth store, service layer"
```

---

## Task 9: Full Backend Test Suite + Compile Verification

- [ ] **Step 9.1: Run complete test suite**

```bash
cd e:/CodeJudgeX/backend && ./mvnw test -q 2>&1 | tail -10
```

Expected: All tests pass (target: 35+ tests, 0 failures, 0 errors).

- [ ] **Step 9.2: Final compile check**

```bash
cd e:/CodeJudgeX/backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9.3: Write session vault files** (follow AGENTS.md shutdown protocol)

Write:
- `.obsidian-ai-memory/01-SESSIONS/2026-05-27/session-0001-claude.md`
- `.obsidian-ai-memory/02-PROJECTS/session-continuity.md` (overwrite)
- `.obsidian-ai-memory/02-PROJECTS/active-goals.md` (check off Week 2 tasks)
- `.obsidian-ai-memory/04-DECISIONS/decisions.md` (append any new decisions)

- [ ] **Step 9.4: Commit code changes (ask user first)**

```bash
cd e:/CodeJudgeX && git add backend/ frontend/
git commit -m "feat(week2): evaluation pipeline, leaderboard, notifications, frontend foundation"
git push origin HEAD
```

- [ ] **Step 9.5: Commit vault (auto)**

```bash
cd e:/CodeJudgeX && git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md
git commit -m "memory: 2026-05-27 claude — Week 2 implementation complete"
git push origin HEAD
```

---

## Self-Review

**Spec coverage check:**
- ✅ `resolveUserId()` fix — Task 1
- ✅ MockMvc controller tests — Task 2
- ✅ Judge0Client + status mapping — Task 3
- ✅ OutputComparator + ScoreCalculator — Task 4
- ✅ EvaluationWorker (consume, evaluate, retry, DLQ) — Task 5
- ✅ LeaderboardService + Controller — Task 6
- ✅ NotificationWorker + EmailService + Controller — Task 7
- ✅ Frontend foundation (types, axios, QueryClient, Zustand, 6 services) — Task 8
- ✅ SubmissionResultRepository — Task 5.1

**Gaps found and fixed:**
- `SubmissionResult` needs `weight` field — added in Task 4.5
- `PageResponse.of(Page<T>)` factory needed — added in Task 7.8
- `judge0client` typo in EvaluationWorker — fixed in Task 5.3
- `EvaluationWorker` needs `LeaderboardService` injected — added in Task 6.3

**Type consistency check:**
- `EvaluationMessage` fields `timeLimitMs`, `memoryLimitMb`, `submissionId`, `problemId`, `contestId` — consistent with existing POJO from Week 1
- `SubmissionResult.setWeight()` added — used in `ScoreCalculator` and `EvaluationWorkerTest`
- `LeaderboardEntryResponse` builder fields used consistently in controller and test
- Frontend `TERMINAL_STATUSES` set exported from `submission.types.ts` — matches backend status strings exactly
