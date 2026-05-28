package com.codejudgex.problem.repository;

import com.codejudgex.problem.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Page<Problem> findByDifficulty(String difficulty, Pageable pageable);

    Page<Problem> findByCreatedBy(UUID createdBy, Pageable pageable);

    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.tags WHERE p.id = :id")
    java.util.Optional<Problem> findByIdWithTags(UUID id);
}
