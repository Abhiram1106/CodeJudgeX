package com.codejudgex.submission.service;

import com.codejudgex.common.exception.BusinessException;
import com.codejudgex.contest.entity.Contest;
import com.codejudgex.contest.repository.ContestParticipantRepository;
import com.codejudgex.contest.repository.ContestRepository;
import com.codejudgex.submission.dto.CreateSubmissionRequest;
import com.codejudgex.submission.dto.SubmissionResponse;
import com.codejudgex.submission.dto.SubmissionStatusResponse;
import com.codejudgex.submission.entity.Submission;
import com.codejudgex.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock ContestRepository contestRepository;
    @Mock ContestParticipantRepository contestParticipantRepository;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks SubmissionService submissionService;

    private UUID studentId;
    private UUID contestId;
    private UUID problemId;
    private Contest liveContest;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        contestId = UUID.randomUUID();
        problemId = UUID.randomUUID();

        liveContest = new Contest();
        liveContest.setStatus("LIVE");
    }

    @Test
    void submit_success_returnsQueuedStatus_andPublishesToRabbit() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setContestId(contestId);
        request.setProblemId(problemId);
        request.setLanguageId(62);
        request.setSourceCode("public class Main {}");

        when(contestRepository.findById(contestId)).thenReturn(Optional.of(liveContest));
        when(contestParticipantRepository.existsByIdContestIdAndIdUserId(contestId, studentId)).thenReturn(true);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = submissionService.submit(request, studentId);

        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(response.getStudentId()).isEqualTo(studentId);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void submit_contestNotLive_throwsBusinessException() {
        Contest draft = new Contest();
        draft.setStatus("DRAFT");

        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setContestId(contestId);
        request.setProblemId(problemId);
        request.setLanguageId(62);
        request.setSourceCode("code");

        when(contestRepository.findById(contestId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> submissionService.submit(request, studentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LIVE");
    }

    @Test
    void submit_notRegistered_throwsBusinessException() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setContestId(contestId);
        request.setProblemId(problemId);
        request.setLanguageId(62);
        request.setSourceCode("code");

        when(contestRepository.findById(contestId)).thenReturn(Optional.of(liveContest));
        when(contestParticipantRepository.existsByIdContestIdAndIdUserId(contestId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.submit(request, studentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not registered");
    }

    @Test
    void getStatus_returnsCurrentStatus() {
        Submission sub = new Submission();
        sub.setStatus("ACCEPTED");
        sub.setScore(100);

        UUID subId = UUID.randomUUID();
        when(submissionRepository.findById(subId)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse status = submissionService.getStatus(subId);

        assertThat(status.getStatus()).isEqualTo("ACCEPTED");
        assertThat(status.getScore()).isEqualTo(100);
    }
}
