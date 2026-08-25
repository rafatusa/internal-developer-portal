package com.enterprise.idp.service;

import com.enterprise.idp.domain.environment.Environment;
import com.enterprise.idp.domain.environment.EnvironmentRepository;
import com.enterprise.idp.domain.environment.EnvironmentType;
import com.enterprise.idp.domain.project.Project;
import com.enterprise.idp.domain.project.ProjectRepository;
import com.enterprise.idp.dto.environment.EnvironmentRequest;
import com.enterprise.idp.dto.environment.EnvironmentResponse;
import com.enterprise.idp.exception.ConflictException;
import com.enterprise.idp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Environment CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ProjectRepository projectRepository;

    /** Create a new environment. */
    @Transactional
    public EnvironmentResponse create(EnvironmentRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        if (environmentRepository.existsByProjectIdAndName(request.getProjectId(), request.getName())) {
            throw new ConflictException(
                "Environment '" + request.getName() + "' already exists in project id="
                    + request.getProjectId());
        }

        Environment env = Environment.builder()
            .name(request.getName())
            .type(request.getType() != null ? request.getType() : EnvironmentType.DEVELOPMENT)
            .url(request.getUrl())
            .cloudProvider(request.getCloudProvider())
            .region(request.getRegion())
            .isProtected(request.isProtected())
            .project(project)
            .build();

        return toResponse(environmentRepository.save(env));
    }

    /** Get environment by ID. */
    @Transactional(readOnly = true)
    public EnvironmentResponse getById(Long id) {
        return toResponse(findById(id));
    }

    /** List all environments paginated. */
    @Transactional(readOnly = true)
    public Page<EnvironmentResponse> getAll(Pageable pageable) {
        return environmentRepository.findAll(pageable).map(this::toResponse);
    }

    /** List environments by project. */
    @Transactional(readOnly = true)
    public List<EnvironmentResponse> getByProject(Long projectId) {
        return environmentRepository.findByProjectId(projectId)
            .stream().map(this::toResponse).toList();
    }

    /** Update an environment. */
    @Transactional
    public EnvironmentResponse update(Long id, EnvironmentRequest request) {
        Environment env = findById(id);

        if (!env.getName().equals(request.getName())
            && environmentRepository.existsByProjectIdAndName(
                env.getProject().getId(), request.getName())) {
            throw new ConflictException(
                "Environment name '" + request.getName() + "' already used in this project");
        }

        env.setName(request.getName());
        env.setType(request.getType());
        env.setUrl(request.getUrl());
        env.setCloudProvider(request.getCloudProvider());
        env.setRegion(request.getRegion());
        env.setProtected(request.isProtected());
        return toResponse(environmentRepository.save(env));
    }

    /** Delete an environment. */
    @Transactional
    public void delete(Long id) {
        Environment env = findById(id);
        environmentRepository.delete(env);
        log.info("Environment '{}' (id={}) deleted", env.getName(), id);
    }

    private Environment findById(Long id) {
        return environmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Environment", id));
    }

    private EnvironmentResponse toResponse(Environment env) {
        EnvironmentResponse resp = new EnvironmentResponse();
        resp.setId(env.getId());
        resp.setName(env.getName());
        resp.setType(env.getType());
        resp.setUrl(env.getUrl());
        resp.setCloudProvider(env.getCloudProvider());
        resp.setRegion(env.getRegion());
        resp.setProtected(env.isProtected());
        resp.setCreatedAt(env.getCreatedAt());
        resp.setUpdatedAt(env.getUpdatedAt());
        if (env.getProject() != null) {
            resp.setProjectId(env.getProject().getId());
            resp.setProjectName(env.getProject().getName());
        }
        return resp;
    }
}
