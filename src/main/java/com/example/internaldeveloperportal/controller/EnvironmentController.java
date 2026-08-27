package com.example.internaldeveloperportal.controller;

import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentRequest;
import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentResponse;
import com.example.internaldeveloperportal.service.EnvironmentService;
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

/** CRUD endpoints for environments. */
@RestController
@RequestMapping("/api/environments")
@Tag(name = "Environments", description = "Deployable environments per project")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping
    @Operation(summary = "List environments, optionally filtered by project")
    public List<EnvironmentResponse> list(@RequestParam(required = false) Long projectId) {
        return environmentService.findAll(projectId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an environment by id")
    public EnvironmentResponse get(@PathVariable Long id) {
        return environmentService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an environment")
    public ResponseEntity<EnvironmentResponse> create(
            @Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse created = environmentService.create(request);
        return ResponseEntity.created(URI.create("/api/environments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an environment")
    public EnvironmentResponse update(@PathVariable Long id,
                                      @Valid @RequestBody EnvironmentRequest request) {
        return environmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an environment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        environmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
