package com.example.internaldeveloperportal.repository;

import com.example.internaldeveloperportal.domain.Environment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Environment}. */
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByProjectId(Long projectId);
}
