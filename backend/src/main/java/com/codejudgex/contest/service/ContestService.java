package com.codejudgex.contest.service;

import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.common.exception.BusinessException;
import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.contest.dto.ContestResponse;
import com.codejudgex.contest.dto.ContestSummaryResponse;
import com.codejudgex.contest.dto.CreateContestRequest;
import com.codejudgex.contest.entity.Contest;
import com.codejudgex.contest.entity.ContestParticipant;
import com.codejudgex.contest.entity.ContestProblem;
import com.codejudgex.contest.repository.ContestParticipantRepository;
import com.codejudgex.contest.repository.ContestProblemRepository;
import com.codejudgex.contest.repository.ContestRepository;
import com.codejudgex.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public ContestResponse createContest(CreateContestRequest request, UUID createdBy) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("End time must be after start time", HttpStatus.BAD_REQUEST);
        }

        Contest contest = new Contest();
        contest.setTitle(request.getTitle());
        contest.setDescription(request.getDescription());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contest.setStatus("DRAFT");
        contest.setCreatedBy(createdBy);

        contestRepository.save(contest);
        return toResponse(contest, List.of(), 0);
    }

    @Transactional
    public ContestResponse addProblem(UUID contestId, UUID problemId, UUID requestedBy) {
        Contest contest = getEditableContest(contestId, requestedBy);

        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Problem", problemId);
        }

        if (contestProblemRepository.existsByIdContestIdAndProblemId(contestId, problemId)) {
            throw new BusinessException("Problem already added to this contest", HttpStatus.CONFLICT);
        }

        int currentCount = contestProblemRepository.findByIdContestIdOrderByProblemOrder(contestId).size();

        ContestProblem cp = new ContestProblem();
        cp.getId().setContestId(contestId);
        cp.getId().setProblemId(problemId);
        cp.setContest(contest);
        cp.setProblemOrder(currentCount);
        contestProblemRepository.save(cp);

        return getContest(contestId);
    }

    @Transactional
    public void registerParticipant(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", contestId));

        if (!"LIVE".equals(contest.getStatus()) && !"UPCOMING".equals(contest.getStatus())) {
            throw new BusinessException("Registration is only open for UPCOMING or LIVE contests", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (contestParticipantRepository.existsByIdContestIdAndIdUserId(contestId, userId)) {
            throw new BusinessException("Already registered for this contest", HttpStatus.CONFLICT);
        }

        ContestParticipant participant = new ContestParticipant();
        participant.getId().setContestId(contestId);
        participant.getId().setUserId(userId);
        participant.setContest(contest);
        contestParticipantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContestSummaryResponse> listContests(String status, Pageable pageable) {
        Page<Contest> page = status != null
                ? contestRepository.findByStatus(status, pageable)
                : contestRepository.findAll(pageable);

        List<ContestSummaryResponse> content = page.getContent().stream()
                .map(c -> ContestSummaryResponse.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .status(c.getStatus())
                        .participantCount(contestParticipantRepository.countByIdContestId(c.getId()))
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<ContestSummaryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ContestResponse getContest(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", contestId));

        List<ContestProblem> problems = contestProblemRepository
                .findByIdContestIdOrderByProblemOrder(contestId);
        long participantCount = contestParticipantRepository.countByIdContestId(contestId);

        return toResponse(contest, problems, participantCount);
    }

    private Contest getEditableContest(UUID contestId, UUID requestedBy) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", contestId));

        if (!"DRAFT".equals(contest.getStatus())) {
            throw new BusinessException("Contest can only be edited in DRAFT status", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return contest;
    }

    private ContestResponse toResponse(Contest contest, List<ContestProblem> problems, long participantCount) {
        List<ContestResponse.ContestProblemEntry> entries = problems.stream()
                .map(cp -> ContestResponse.ContestProblemEntry.builder()
                        .problemId(cp.getProblemId())
                        .problemOrder(cp.getProblemOrder())
                        .build())
                .collect(Collectors.toList());

        return ContestResponse.builder()
                .id(contest.getId())
                .title(contest.getTitle())
                .description(contest.getDescription())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .status(contest.getStatus())
                .createdBy(contest.getCreatedBy())
                .participantCount(participantCount)
                .problems(entries)
                .createdAt(contest.getCreatedAt())
                .build();
    }
}
