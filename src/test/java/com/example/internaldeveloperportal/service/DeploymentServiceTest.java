package com.example.internaldeveloperportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.internaldeveloperportal.domain.Deployment;
import com.example.internaldeveloperportal.domain.Environment;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentRequest;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentResponse;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.DeploymentRepository;
import com.example.internaldeveloperportal.repository.EnvironmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private DeploymentService deploymentService;

    @Test
    @DisplayName("filtering by environment returns newest deployments first")
    void listByEnvironment() {
        Deployment deployment = new Deployment();
        deployment.setId(1L);
        deployment.setVersion("1.4.2");
        deployment.setStatus(Deployment.Status.SUCCEEDED);
        when(deploymentRepository.findByEnvironmentIdOrderByCreatedAtDesc(3L))
            .thenReturn(List.of(deployment));

        List<DeploymentResponse> result = deploymentService.findAll(3L);

        assertThat(result).singleElement()
            .satisfies(item -> {
                assertThat(item.version()).isEqualTo("1.4.2");
                assertThat(item.status()).isEqualTo(Deployment.Status.SUCCEEDED);
            });
    }

    @Test
    @DisplayName("create attaches the deployment to its environment")
    void createAttachesEnvironment() {
        Environment environment = new Environment();
        environment.setId(3L);
        environment.setName("prod");
        when(environmentRepository.findById(3L)).thenReturn(Optional.of(environment));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(call -> {
            Deployment saved = call.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        DeploymentResponse created = deploymentService.create(new DeploymentRequest(
            "1.4.2", "abc1234", Deployment.Status.RUNNING, "ci-bot", 3L));

        assertThat(created.id()).isEqualTo(21L);
        assertThat(created.environmentName()).isEqualTo("prod");
        assertThat(created.triggeredBy()).isEqualTo("ci-bot");
    }

    @Test
    @DisplayName("create fails for an unknown environment")
    void createUnknownEnvironment() {
        when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deploymentService.create(new DeploymentRequest(
            "1.0.0", null, Deployment.Status.PENDING, null, 99L)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Environment");
    }

    @Test
    @DisplayName("update transitions the recorded status")
    void updateTransitionsStatus() {
        Deployment existing = new Deployment();
        existing.setId(21L);
        existing.setVersion("1.4.2");
        existing.setStatus(Deployment.Status.RUNNING);
        when(deploymentRepository.findById(21L)).thenReturn(Optional.of(existing));
        when(deploymentRepository.save(any(Deployment.class)))
            .thenAnswer(call -> call.getArgument(0));

        DeploymentResponse updated = deploymentService.update(21L, new DeploymentRequest(
            "1.4.2", "abc1234", Deployment.Status.SUCCEEDED, "ci-bot", null));

        assertThat(updated.status()).isEqualTo(Deployment.Status.SUCCEEDED);
    }

    @Test
    @DisplayName("findById raises not-found for an unknown deployment")
    void findByIdMissing() {
        when(deploymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deploymentService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
