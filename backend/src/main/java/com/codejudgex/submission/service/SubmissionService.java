package com.codejudgex.submission.service;

import com.codejudgex.common.exception.BusinessException;
import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.contest.entity.Contest;
import com.codejudgex.contest.repository.ContestParticipantRepository;
import com.codejudgex.contest.repository.ContestRepository;
import com.codejudgex.infrastructure.config.RabbitMQConfig;
import com.codejudgex.submission.dto.CreateSubmissionRequest;
import com.codejudgex.submission.dto.EvaluationMessage;
import com.codejudgex.submission.dto.SubmissionResponse;
import com.codejudgex.submission.dto.SubmissionStatusResponse;
import com.codejudgex.submission.entity.Submission;
import com.codejudgex.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ContestRepository contestRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public SubmissionResponse submit(CreateSubmissionRequest request, UUID studentId) {
        Contest contest = contestRepository.findById(request.getContestId())
                .orElseThrow(() -> new ResourceNotFoundException("Contest", request.getContestId()));

        if (!"LIVE".equals(contest.getStatus())) {
            throw new BusinessException("Submissions are only accepted for LIVE contests", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!contestParticipantRepository.existsByIdContestIdAndIdUserId(request.getContestId(), studentId)) {
            throw new BusinessException("You are not registered for this contest", HttpStatus.FORBIDDEN);
        }

        Submission submission = new Submission();
        submission.setStudentId(studentId);
        submission.setContestId(request.getContestId());
        submission.setProblemId(request.getProblemId());
        submission.setLanguageId(request.getLanguageId());
        submission.setSourceCode(request.getSourceCode());
        submission.setSourceCodeHash(hashSourceCode(request.getSourceCode()));
        submission.setStatus("QUEUED");

        submissionRepository.save(submission);

        EvaluationMessage message = EvaluationMessage.builder()
                .submissionId(submission.getId())
                .problemId(submission.getProblemId())
                .contestId(submission.getContestId())
                .studentId(submission.getStudentId())
                .languageId(submission.getLanguageId())
                .sourceCode(submission.getSourceCode())
                .attemptNumber(1)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_SUBMISSION_CREATED,
                message);

        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionStatusResponse getStatus(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        return SubmissionStatusResponse.builder()
                .id(submission.getId())
                .status(submission.getStatus())
                .score(submission.getScore())
                .build();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, UUID studentId) {
        Submission submission = submissionRepository.findByIdAndStudentId(submissionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        return toResponse(submission);
    }

    private SubmissionResponse toResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .studentId(s.getStudentId())
                .contestId(s.getContestId())
                .problemId(s.getProblemId())
                .languageId(s.getLanguageId())
                .status(s.getStatus())
                .score(s.getScore())
                .executionTimeMs(s.getExecutionTimeMs())
                .memoryUsedMb(s.getMemoryUsedMb())
                .submittedAt(s.getSubmittedAt())
                .evaluatedAt(s.getEvaluatedAt())
                .build();
    }

    private String hashSourceCode(String sourceCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceCode.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
