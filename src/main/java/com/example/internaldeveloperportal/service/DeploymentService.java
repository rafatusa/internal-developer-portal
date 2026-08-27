package com.example.internaldeveloperportal.service;

import com.example.internaldeveloperportal.domain.Deployment;
import com.example.internaldeveloperportal.domain.Environment;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentRequest;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentResponse;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.DeploymentRepository;
import com.example.internaldeveloperportal.repository.EnvironmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD operations for deployment records. */
@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final EnvironmentRepository environmentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository,
                             EnvironmentRepository environmentRepository) {
        this.deploymentRepository = deploymentRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> findAll(Long environmentId) {
        List<Deployment> deployments = environmentId == null
            ? deploymentRepository.findAll()
            : deploymentRepository.findByEnvironmentIdOrderByCreatedAtDesc(environmentId);
        return deployments.stream().map(DeploymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeploymentResponse findById(Long id) {
        return DeploymentResponse.from(require(id));
    }

    @Transactional
    public DeploymentResponse create(DeploymentRequest request) {
        Deployment deployment = new Deployment();
        apply(deployment, request);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Transactional
    public DeploymentResponse update(Long id, DeploymentRequest request) {
        Deployment deployment = require(id);
        apply(deployment, request);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Transactional
    public void delete(Long id) {
        deploymentRepository.delete(require(id));
    }

    private Deployment require(Long id) {
        return deploymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));
    }

    private void apply(Deployment deployment, DeploymentRequest request) {
        deployment.setVersion(request.version());
        deployment.setCommitSha(request.commitSha());
        deployment.setStatus(request.status());
        deployment.setTriggeredBy(request.triggeredBy());
        if (request.environmentId() == null) {
            deployment.setEnvironment(null);
        } else {
            Environment environment = environmentRepository.findById(request.environmentId())
                .orElseThrow(() ->
                    new ResourceNotFoundException("Environment", request.environmentId()));
            deployment.setEnvironment(environment);
        }
    }
}
