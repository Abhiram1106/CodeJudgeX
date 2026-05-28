package com.codejudgex.problem.repository;

import com.codejudgex.problem.entity.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemTagRepository extends JpaRepository<ProblemTag, UUID> {

    Optional<ProblemTag> findByName(String name);
}
