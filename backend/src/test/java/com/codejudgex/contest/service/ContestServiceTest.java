package com.codejudgex.contest.service;

import com.codejudgex.common.exception.BusinessException;
import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.contest.dto.ContestResponse;
import com.codejudgex.contest.dto.CreateContestRequest;
import com.codejudgex.contest.entity.Contest;
import com.codejudgex.contest.entity.ContestParticipant;
import com.codejudgex.contest.repository.ContestParticipantRepository;
import com.codejudgex.contest.repository.ContestProblemRepository;
import com.codejudgex.contest.repository.ContestRepository;
import com.codejudgex.problem.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock ContestRepository contestRepository;
    @Mock ContestProblemRepository contestProblemRepository;
    @Mock ContestParticipantRepository contestParticipantRepository;
    @Mock ProblemRepository problemRepository;

    @InjectMocks ContestService contestService;

    private UUID creatorId;
    private Instant startTime;
    private Instant endTime;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        startTime = Instant.now().plus(1, ChronoUnit.HOURS);
        endTime   = Instant.now().plus(3, ChronoUnit.HOURS);
    }

    @Test
    void createContest_success() {
        CreateContestRequest request = new CreateContestRequest();
        request.setTitle("Spring Contest 2026");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(contestRepository.save(any(Contest.class))).thenAnswer(inv -> inv.getArgument(0));

        ContestResponse response = contestService.createContest(request, creatorId);

        assertThat(response.getTitle()).isEqualTo("Spring Contest 2026");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
        assertThat(response.getCreatedBy()).isEqualTo(creatorId);
    }

    @Test
    void createContest_endBeforeStart_throwsBusinessException() {
        CreateContestRequest request = new CreateContestRequest();
        request.setTitle("Bad Contest");
        request.setStartTime(endTime);
        request.setEndTime(startTime); // end < start

        assertThatThrownBy(() -> contestService.createContest(request, creatorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End time");
    }

    @Test
    void registerParticipant_contestNotLive_throwsBusinessException() {
        Contest contest = new Contest();
        contest.setStatus("DRAFT");

        UUID contestId = UUID.randomUUID();
        UUID userId    = UUID.randomUUID();

        when(contestRepository.findById(contestId)).thenReturn(Optional.of(contest));

        assertThatThrownBy(() -> contestService.registerParticipant(contestId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Registration");
    }

    @Test
    void registerParticipant_alreadyRegistered_throwsBusinessException() {
        Contest contest = new Contest();
        contest.setStatus("LIVE");

        UUID contestId = UUID.randomUUID();
        UUID userId    = UUID.randomUUID();

        when(contestRepository.findById(contestId)).thenReturn(Optional.of(contest));
        when(contestParticipantRepository.existsByIdContestIdAndIdUserId(contestId, userId)).thenReturn(true);

        assertThatThrownBy(() -> contestService.registerParticipant(contestId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Already registered");
    }

    @Test
    void getContest_notFound_throwsResourceNotFoundException() {
        UUID contestId = UUID.randomUUID();
        when(contestRepository.findById(contestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contestService.getContest(contestId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
