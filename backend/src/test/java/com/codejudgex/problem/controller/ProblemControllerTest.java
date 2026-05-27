package com.codejudgex.problem.controller;

import com.codejudgex.auth.filter.JwtAuthenticationFilter;
import com.codejudgex.auth.service.JwtService;
import com.codejudgex.infrastructure.config.SecurityConfig;
import com.codejudgex.problem.dto.CreateProblemRequest;
import com.codejudgex.problem.dto.ProblemResponse;
import com.codejudgex.problem.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-key-min-32-chars-xxxxxx",
        "app.jwt.access-token-expiry-ms=900000"
})
class ProblemControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProblemService problemService;
    @MockBean JwtService jwtService;

    // UUID string so resolveUserId() can parse principal.getName()
    private static final String FACULTY_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String STUDENT_UUID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private ProblemResponse sampleProblem() {
        return ProblemResponse.builder()
                .id(UUID.randomUUID())
                .title("Two Sum")
                .description("Find two numbers that add up to target")
                .difficulty("EASY")
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .tags(Set.of("arrays"))
                .sampleTestCases(List.of())
                .build();
    }

    private CreateProblemRequest validCreateRequest() {
        CreateProblemRequest r = new CreateProblemRequest();
        r.setTitle("Two Sum");
        r.setDescription("Find two numbers that add up to target");
        r.setDifficulty("EASY");
        r.setTimeLimitMs(2000);
        r.setMemoryLimitMb(256);
        return r;
    }

    @Test
    @WithMockUser(username = FACULTY_UUID, roles = "FACULTY")
    void createProblem_asFaculty_returns201() throws Exception {
        when(problemService.createProblem(any(), any())).thenReturn(sampleProblem());

        mockMvc.perform(post("/problems")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Two Sum"))
                .andExpect(jsonPath("$.data.difficulty").value("EASY"));
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void createProblem_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/problems")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void listProblems_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk());
    }

    @Test
    void listProblems_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isUnauthorized());
    }
}
