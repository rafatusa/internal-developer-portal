package com.example.internaldeveloperportal.dto;

import com.example.internaldeveloperportal.domain.Team;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads for team resources. */
public final class TeamDtos {

    private TeamDtos() {
    }

    /** Payload for creating or replacing a team. */
    public record TeamRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Email @Size(max = 200) String contactEmail) {
    }

    /** Team representation returned to clients. */
    public record TeamResponse(
        Long id,
        String name,
        String description,
        String contactEmail,
        Instant createdAt) {

        public static TeamResponse from(Team team) {
            return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getContactEmail(),
                team.getCreatedAt());
        }
    }
}
