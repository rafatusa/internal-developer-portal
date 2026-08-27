package com.example.internaldeveloperportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.internaldeveloperportal.domain.Project;
import com.example.internaldeveloperportal.domain.Team;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectRequest;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectResponse;
import com.example.internaldeveloperportal.exception.ConflictException;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.ProjectRepository;
import com.example.internaldeveloperportal.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("listing without a team filter reads all projects")
    void listAll() {
        Project project = new Project();
        project.setId(1L);
        project.setName("checkout");
        when(projectRepository.findAll()).thenReturn(List.of(project));

        List<ProjectResponse> result = projectService.findAll(null);

        assertThat(result).singleElement()
            .satisfies(item -> assertThat(item.name()).isEqualTo("checkout"));
    }

    @Test
    @DisplayName("listing with a team filter delegates to findByTeamId")
    void listByTeam() {
        when(projectRepository.findByTeamId(4L)).thenReturn(List.of());

        assertThat(projectService.findAll(4L)).isEmpty();
    }

    @Test
    @DisplayName("create links the project to an existing team")
    void createLinksTeam() {
        Team team = new Team();
        team.setId(4L);
        team.setName("platform");
        when(projectRepository.existsByName("checkout")).thenReturn(false);
        when(teamRepository.findById(4L)).thenReturn(Optional.of(team));
        when(projectRepository.save(any(Project.class))).thenAnswer(call -> {
            Project saved = call.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        ProjectResponse created = projectService.create(
            new ProjectRequest("checkout", "Checkout service", "https://git/checkout", "java", 4L));

        assertThat(created.id()).isEqualTo(11L);
        assertThat(created.teamId()).isEqualTo(4L);
        assertThat(created.teamName()).isEqualTo("platform");
    }

    @Test
    @DisplayName("create fails when the referenced team does not exist")
    void createUnknownTeam() {
        when(projectRepository.existsByName("checkout")).thenReturn(false);
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(
            new ProjectRequest("checkout", null, null, null, 99L)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Team");
    }

    @Test
    @DisplayName("create rejects a duplicate project name")
    void createDuplicate() {
        when(projectRepository.existsByName("checkout")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(
            new ProjectRequest("checkout", null, null, null, null)))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("update can detach the project from its team")
    void updateDetachesTeam() {
        Team team = new Team();
        team.setId(4L);
        Project existing = new Project();
        existing.setId(1L);
        existing.setName("checkout");
        existing.setTeam(team);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));

        ProjectResponse updated = projectService.update(1L,
            new ProjectRequest("checkout", null, null, null, null));

        assertThat(updated.teamId()).isNull();
    }
}
