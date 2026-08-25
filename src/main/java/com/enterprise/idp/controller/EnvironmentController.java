package com.enterprise.idp.controller;

import com.enterprise.idp.dto.environment.EnvironmentRequest;
import com.enterprise.idp.dto.environment.EnvironmentResponse;
import com.enterprise.idp.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Environment CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/environments")
@RequiredArgsConstructor
@Tag(name = "Environments", description = "CRUD operations for deployment environments")
@SecurityRequirement(name = "bearerAuth")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping
    @Operation(summary = "Create a new environment")
    public ResponseEntity<EnvironmentResponse> create(
        @Valid @RequestBody EnvironmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(environmentService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get environment by ID")
    public ResponseEntity<EnvironmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(environmentService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all environments (paginated)")
    public ResponseEntity<Page<EnvironmentResponse>> getAll(
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(environmentService.getAll(pageable));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List environments by project")
    public ResponseEntity<List<EnvironmentResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(environmentService.getByProject(projectId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an environment")
    public ResponseEntity<EnvironmentResponse> update(
        @PathVariable Long id, @Valid @RequestBody EnvironmentRequest request) {
        return ResponseEntity.ok(environmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an environment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        environmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
