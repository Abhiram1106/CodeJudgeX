package com.codejudgex.contest.repository;

import com.codejudgex.contest.entity.ContestParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, ContestParticipant.ContestParticipantId> {

    boolean existsByIdContestIdAndIdUserId(UUID contestId, UUID userId);

    long countByIdContestId(UUID contestId);
}
