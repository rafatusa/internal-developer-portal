package com.example.internaldeveloperportal.repository;

import com.example.internaldeveloperportal.domain.Deployment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Deployment}. */
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    List<Deployment> findByEnvironmentIdOrderByCreatedAtDesc(Long environmentId);
}
