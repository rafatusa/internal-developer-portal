package com.example.internaldeveloperportal.repository;

import com.example.internaldeveloperportal.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Team}. */
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);
}
