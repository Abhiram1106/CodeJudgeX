package com.codejudgex.submission.repository;

import com.codejudgex.submission.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Page<Submission> findByStudentIdAndContestId(UUID studentId, UUID contestId, Pageable pageable);

    Page<Submission> findByContestId(UUID contestId, Pageable pageable);

    Optional<Submission> findByIdAndStudentId(UUID id, UUID studentId);
}
