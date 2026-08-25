package com.enterprise.idp.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a Team.
 */
@Data
@Schema(description = "Team details")
public class TeamResponse {

    @Schema(description = "Team ID")
    private Long id;

    @Schema(description = "Team name")
    private String name;

    @Schema(description = "Team description")
    private String description;

    @Schema(description = "Slack channel")
    private String slackChannel;

    @Schema(description = "Email distribution")
    private String emailDistribution;

    @Schema(description = "Member count")
    private Integer memberCount;

    @Schema(description = "Created at")
    private Instant createdAt;

    @Schema(description = "Updated at")
    private Instant updatedAt;
}
