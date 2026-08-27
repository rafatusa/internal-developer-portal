package com.example.internaldeveloperportal.controller;

import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentRequest;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentResponse;
import com.example.internaldeveloperportal.service.DeploymentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CRUD endpoints for deployment records. */
@RestController
@RequestMapping("/api/deployments")
@Tag(name = "Deployments", description = "Release records per environment")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    @Operation(summary = "List deployments, optionally filtered by environment")
    public List<DeploymentResponse> list(@RequestParam(required = false) Long environmentId) {
        return deploymentService.findAll(environmentId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a deployment by id")
    public DeploymentResponse get(@PathVariable Long id) {
        return deploymentService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Record a deployment")
    public ResponseEntity<DeploymentResponse> create(
            @Valid @RequestBody DeploymentRequest request) {
        DeploymentResponse created = deploymentService.create(request);
        return ResponseEntity.created(URI.create("/api/deployments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a deployment record")
    public DeploymentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody DeploymentRequest request) {
        return deploymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deployment record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deploymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
