package com.example.internaldeveloperportal.service;

import com.example.internaldeveloperportal.domain.Environment;
import com.example.internaldeveloperportal.domain.Project;
import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentRequest;
import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentResponse;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.EnvironmentRepository;
import com.example.internaldeveloperportal.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD operations for environments. */
@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ProjectRepository projectRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository,
                              ProjectRepository projectRepository) {
        this.environmentRepository = environmentRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> findAll(Long projectId) {
        List<Environment> environments = projectId == null
            ? environmentRepository.findAll()
            : environmentRepository.findByProjectId(projectId);
        return environments.stream().map(EnvironmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse findById(Long id) {
        return EnvironmentResponse.from(require(id));
    }

    @Transactional
    public EnvironmentResponse create(EnvironmentRequest request) {
        Environment environment = new Environment();
        apply(environment, request);
        return EnvironmentResponse.from(environmentRepository.save(environment));
    }

    @Transactional
    public EnvironmentResponse update(Long id, EnvironmentRequest request) {
        Environment environment = require(id);
        apply(environment, request);
        return EnvironmentResponse.from(environmentRepository.save(environment));
    }

    @Transactional
    public void delete(Long id) {
        environmentRepository.delete(require(id));
    }

    private Environment require(Long id) {
        return environmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Environment", id));
    }

    private void apply(Environment environment, EnvironmentRequest request) {
        environment.setName(request.name());
        environment.setTier(request.tier());
        environment.setRegion(request.region());
        environment.setEndpointUrl(request.endpointUrl());
        if (request.projectId() == null) {
            environment.setProject(null);
        } else {
            Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.projectId()));
            environment.setProject(project);
        }
    }
}
