package com.enterprise.idp.service;

import com.enterprise.idp.domain.deployment.Deployment;
import com.enterprise.idp.domain.deployment.DeploymentRepository;
import com.enterprise.idp.domain.deployment.DeploymentStatus;
import com.enterprise.idp.domain.environment.Environment;
import com.enterprise.idp.domain.environment.EnvironmentRepository;
import com.enterprise.idp.domain.project.Project;
import com.enterprise.idp.domain.project.ProjectRepository;
import com.enterprise.idp.dto.deployment.DeploymentRequest;
import com.enterprise.idp.dto.deployment.DeploymentResponse;
import com.enterprise.idp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Deployment CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;

    /** Create a new deployment record. */
    @Transactional
    public DeploymentResponse create(DeploymentRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        Environment environment = environmentRepository.findById(request.getEnvironmentId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Environment", request.getEnvironmentId()));

        Deployment deployment = Deployment.builder()
            .version(request.getVersion())
            .status(request.getStatus() != null ? request.getStatus() : DeploymentStatus.PENDING)
            .commitSha(request.getCommitSha())
            .deployedBy(request.getDeployedBy())
            .pipelineUrl(request.getPipelineUrl())
            .notes(request.getNotes())
            .startedAt(request.getStartedAt())
            .completedAt(request.getCompletedAt())
            .project(project)
            .environment(environment)
            .build();

        return toResponse(deploymentRepository.save(deployment));
    }

    /** Get deployment by ID. */
    @Transactional(readOnly = true)
    public DeploymentResponse getById(Long id) {
        return toResponse(findById(id));
    }

    /** List all deployments paginated. */
    @Transactional(readOnly = true)
    public Page<DeploymentResponse> getAll(Pageable pageable) {
        return deploymentRepository.findAll(pageable).map(this::toResponse);
    }

    /** List deployments by project. */
    @Transactional(readOnly = true)
    public Page<DeploymentResponse> getByProject(Long projectId, Pageable pageable) {
        return deploymentRepository.findByProjectId(projectId, pageable).map(this::toResponse);
    }

    /** List deployments by status. */
    @Transactional(readOnly = true)
    public Page<DeploymentResponse> getByStatus(DeploymentStatus status, Pageable pageable) {
        return deploymentRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    /** Update deployment record. */
    @Transactional
    public DeploymentResponse update(Long id, DeploymentRequest request) {
        Deployment deployment = findById(id);

        deployment.setVersion(request.getVersion());
        deployment.setStatus(request.getStatus() != null ? request.getStatus() : deployment.getStatus());
        deployment.setCommitSha(request.getCommitSha());
        deployment.setDeployedBy(request.getDeployedBy());
        deployment.setPipelineUrl(request.getPipelineUrl());
        deployment.setNotes(request.getNotes());
        deployment.setStartedAt(request.getStartedAt());
        deployment.setCompletedAt(request.getCompletedAt());

        return toResponse(deploymentRepository.save(deployment));
    }

    /** Delete a deployment record. */
    @Transactional
    public void delete(Long id) {
        Deployment deployment = findById(id);
        deploymentRepository.delete(deployment);
        log.info("Deployment id={} for project '{}' deleted",
            id, deployment.getProject().getName());
    }

    private Deployment findById(Long id) {
        return deploymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));
    }

    private DeploymentResponse toResponse(Deployment d) {
        DeploymentResponse resp = new DeploymentResponse();
        resp.setId(d.getId());
        resp.setVersion(d.getVersion());
        resp.setStatus(d.getStatus());
        resp.setCommitSha(d.getCommitSha());
        resp.setDeployedBy(d.getDeployedBy());
        resp.setPipelineUrl(d.getPipelineUrl());
        resp.setNotes(d.getNotes());
        resp.setStartedAt(d.getStartedAt());
        resp.setCompletedAt(d.getCompletedAt());
        resp.setCreatedAt(d.getCreatedAt());
        resp.setUpdatedAt(d.getUpdatedAt());
        if (d.getProject() != null) {
            resp.setProjectId(d.getProject().getId());
            resp.setProjectName(d.getProject().getName());
        }
        if (d.getEnvironment() != null) {
            resp.setEnvironmentId(d.getEnvironment().getId());
            resp.setEnvironmentName(d.getEnvironment().getName());
        }
        return resp;
    }
}
