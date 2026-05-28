package com.codejudgex.contest.repository;

import com.codejudgex.contest.entity.Contest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {

    Page<Contest> findByStatus(String status, Pageable pageable);

    Page<Contest> findByCreatedBy(UUID createdBy, Pageable pageable);
}
