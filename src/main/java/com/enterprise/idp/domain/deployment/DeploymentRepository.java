package com.enterprise.idp.domain.deployment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Deployment entities.
 */
@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    Page<Deployment> findByProjectId(Long projectId, Pageable pageable);

    Page<Deployment> findByEnvironmentId(Long environmentId, Pageable pageable);

    Page<Deployment> findByStatus(DeploymentStatus status, Pageable pageable);

    @Query("SELECT d FROM Deployment d WHERE d.project.id = :projectId "
        + "AND d.environment.id = :envId ORDER BY d.createdAt DESC")
    Page<Deployment> findByProjectAndEnvironment(
        @Param("projectId") Long projectId,
        @Param("envId") Long envId,
        Pageable pageable);

    @Query("SELECT d FROM Deployment d WHERE d.project.id = :projectId "
        + "AND d.environment.id = :envId ORDER BY d.createdAt DESC LIMIT 1")
    Optional<Deployment> findLatestByProjectAndEnvironment(
        @Param("projectId") Long projectId,
        @Param("envId") Long envId);
}
