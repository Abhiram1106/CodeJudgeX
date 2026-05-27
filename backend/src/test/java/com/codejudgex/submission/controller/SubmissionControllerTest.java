package com.codejudgex.submission.controller;

import com.codejudgex.auth.filter.JwtAuthenticationFilter;
import com.codejudgex.auth.service.JwtService;
import com.codejudgex.infrastructure.config.SecurityConfig;
import com.codejudgex.submission.dto.CreateSubmissionRequest;
import com.codejudgex.submission.dto.SubmissionResponse;
import com.codejudgex.submission.dto.SubmissionStatusResponse;
import com.codejudgex.submission.service.SubmissionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-key-min-32-chars-xxxxxx",
        "app.jwt.access-token-expiry-ms=900000"
})
class SubmissionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SubmissionService submissionService;
    @MockBean JwtService jwtService;

    private static final String STUDENT_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String FACULTY_UUID = "22222222-2222-2222-2222-222222222222";
    private static final UUID   SUBMISSION_ID = UUID.randomUUID();

    private CreateSubmissionRequest validSubmitRequest() {
        CreateSubmissionRequest r = new CreateSubmissionRequest();
        r.setContestId(UUID.randomUUID());
        r.setProblemId(UUID.randomUUID());
        r.setLanguageId(62);
        r.setSourceCode("public class Main { public static void main(String[] a) { System.out.println(1); } }");
        return r;
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void submit_asStudent_returns202() throws Exception {
        SubmissionResponse response = SubmissionResponse.builder()
                .id(SUBMISSION_ID)
                .status("QUEUED")
                .score(0)
                .languageId(62)
                .submittedAt(Instant.now())
                .build();

        when(submissionService.submit(any(), any())).thenReturn(response);

        mockMvc.perform(post("/submissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSubmitRequest())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.id").value(SUBMISSION_ID.toString()));
    }

    @Test
    @WithMockUser(username = FACULTY_UUID, roles = "FACULTY")
    void submit_asFaculty_returns403() throws Exception {
        mockMvc.perform(post("/submissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSubmitRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void submit_missingSourceCode_returns400() throws Exception {
        CreateSubmissionRequest r = new CreateSubmissionRequest();
        r.setContestId(UUID.randomUUID());
        r.setProblemId(UUID.randomUUID());
        r.setLanguageId(62);
        // sourceCode intentionally missing

        mockMvc.perform(post("/submissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = STUDENT_UUID, roles = "STUDENT")
    void getStatus_authenticated_returns200() throws Exception {
        SubmissionStatusResponse statusResponse = SubmissionStatusResponse.builder()
                .id(SUBMISSION_ID)
                .status("ACCEPTED")
                .score(100)
                .build();

        when(submissionService.getStatus(SUBMISSION_ID)).thenReturn(statusResponse);

        mockMvc.perform(get("/submissions/{id}/status", SUBMISSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.score").value(100));
    }

    @Test
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/submissions/{id}/status", SUBMISSION_ID))
                .andExpect(status().isUnauthorized());
    }
}
