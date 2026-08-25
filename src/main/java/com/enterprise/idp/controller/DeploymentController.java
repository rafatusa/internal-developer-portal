package com.enterprise.idp.controller;

import com.enterprise.idp.domain.deployment.DeploymentStatus;
import com.enterprise.idp.dto.deployment.DeploymentRequest;
import com.enterprise.idp.dto.deployment.DeploymentResponse;
import com.enterprise.idp.service.DeploymentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Deployment CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
@Tag(name = "Deployments", description = "CRUD operations for deployment events")
@SecurityRequirement(name = "bearerAuth")
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    @Operation(summary = "Record a new deployment")
    public ResponseEntity<DeploymentResponse> create(
        @Valid @RequestBody DeploymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deploymentService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deployment by ID")
    public ResponseEntity<DeploymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deploymentService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all deployments (paginated)")
    public ResponseEntity<Page<DeploymentResponse>> getAll(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) DeploymentStatus status) {
        if (projectId != null) {
            return ResponseEntity.ok(deploymentService.getByProject(projectId, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(deploymentService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(deploymentService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a deployment record")
    public ResponseEntity<DeploymentResponse> update(
        @PathVariable Long id, @Valid @RequestBody DeploymentRequest request) {
        return ResponseEntity.ok(deploymentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deployment record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deploymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
