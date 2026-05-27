package com.codejudgex.contest.controller;

import com.codejudgex.auth.filter.JwtAuthenticationFilter;
import com.codejudgex.auth.service.JwtService;
import com.codejudgex.contest.dto.ContestResponse;
import com.codejudgex.contest.dto.CreateContestRequest;
import com.codejudgex.contest.service.ContestService;
import com.codejudgex.infrastructure.config.SecurityConfig;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-key-min-32-chars-xxxxxx",
        "app.jwt.access-token-expiry-ms=900000"
})
class ContestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ContestService contestService;
    @MockBean JwtService jwtService;

    private static final String FACULTY_UUID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String STUDENT_UUID = "dddddddd-dddd-dddd-dddd-dddddddddddd";

    private ContestResponse sampleContest() {
        return ContestResponse.builder()
                .id(UUID.randomUUID())
                .title("Spring Contest")
                .description("A test contest")
                .status("DRAFT")
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .participantCount(0)
                .problems(List.of())
                .build();
    }

    private CreateContestRequest validCreateRequest() {
        CreateContestRequest r = new CreateContestRequest();
        r.setTitle("Spring Contest");
        r.setDescription("A test contest");
        r.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        r.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS));
        return r;
    }

    @Test
    @WithMockUser(username = FACULTY_UUID, roles = "FACULTY")
    void createContest_asFaculty_returns201() throws Exception {
        when(contestService.createContest(any(), any())).thenReturn(sampleContest());

        mockMvc.perform(post("/contests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Spring Contest"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void createContest_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/contests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void listContests_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/contests"))
                .andExpect(status().isOk());
    }

    @Test
    void listContests_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/contests"))
                .andExpect(status().isUnauthorized());
    }
}
