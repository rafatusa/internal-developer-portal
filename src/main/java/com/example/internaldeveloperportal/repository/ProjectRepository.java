package com.example.internaldeveloperportal.repository;

import com.example.internaldeveloperportal.domain.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Project}. */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByName(String name);

    List<Project> findByTeamId(Long teamId);
}
