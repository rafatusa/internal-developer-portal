package com.enterprise.idp.domain.environment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Environment entities.
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByProjectId(Long projectId);

    Page<Environment> findByProjectId(Long projectId, Pageable pageable);

    boolean existsByProjectIdAndName(Long projectId, String name);

    Page<Environment> findByType(EnvironmentType type, Pageable pageable);
}
