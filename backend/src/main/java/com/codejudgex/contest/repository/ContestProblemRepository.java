package com.codejudgex.contest.repository;

import com.codejudgex.contest.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, ContestProblem.ContestProblemId> {

    List<ContestProblem> findByIdContestIdOrderByProblemOrder(UUID contestId);

    boolean existsByIdContestIdAndProblemId(UUID contestId, UUID problemId);
}
