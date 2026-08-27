package com.example.internaldeveloperportal.repository;

import com.example.internaldeveloperportal.domain.PortalUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PortalUser}. */
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
