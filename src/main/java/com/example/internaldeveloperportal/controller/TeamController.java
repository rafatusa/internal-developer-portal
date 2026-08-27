package com.example.internaldeveloperportal.controller;

import com.example.internaldeveloperportal.dto.TeamDtos.TeamRequest;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamResponse;
import com.example.internaldeveloperportal.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD endpoints for teams. */
@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "Engineering teams that own projects")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @Operation(summary = "List all teams")
    public List<TeamResponse> list() {
        return teamService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a team by id")
    public TeamResponse get(@PathVariable Long id) {
        return teamService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a team")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        TeamResponse created = teamService.create(request);
        return ResponseEntity.created(URI.create("/api/teams/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a team")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a team")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
