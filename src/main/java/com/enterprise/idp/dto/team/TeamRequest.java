package com.enterprise.idp.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating or updating a Team.
 */
@Data
@Schema(description = "Team creation/update payload")
public class TeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    @Schema(description = "Unique team name", example = "Platform Engineering")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Team description", example = "Core platform infrastructure team")
    private String description;

    @Schema(description = "Slack channel", example = "#platform-eng")
    private String slackChannel;

    @Schema(description = "Email distribution", example = "platform-eng@enterprise.com")
    private String emailDistribution;

    @Schema(description = "Current member count", example = "8")
    private Integer memberCount;
}
