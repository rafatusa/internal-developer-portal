package com.enterprise.idp.service;

import com.enterprise.idp.domain.project.Project;
import com.enterprise.idp.domain.project.ProjectRepository;
import com.enterprise.idp.domain.project.ProjectStatus;
import com.enterprise.idp.domain.team.Team;
import com.enterprise.idp.domain.team.TeamRepository;
import com.enterprise.idp.dto.project.ProjectRequest;
import com.enterprise.idp.dto.project.ProjectResponse;
import com.enterprise.idp.exception.ConflictException;
import com.enterprise.idp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project project;
    private ProjectRequest request;

    @BeforeEach
    void setUp() {
        project = Project.builder()
            .name("Test Project")
            .description("A test project")
            .status(ProjectStatus.ACTIVE)
            .repoUrl("https://github.com/enterprise/test")
            .techStack("Spring Boot")
            .build();
        project.setId(1L);

        request = new ProjectRequest();
        request.setName("Test Project");
        request.setDescription("A test project");
        request.setStatus(ProjectStatus.ACTIVE);
        request.setRepoUrl("https://github.com/enterprise/test");
        request.setTechStack("Spring Boot");
    }

    @Test
    @DisplayName("create() — persists project and returns response")
    void create_success() {
        when(projectRepository.existsByName(anyString())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Project");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("create() — throws ConflictException when name already exists")
    void create_duplicateName_throwsConflict() {
        when(projectRepository.existsByName("Test Project")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test Project");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById() — returns project when found")
    void getById_found() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("getById() — throws ResourceNotFoundException when not found")
    void getById_notFound_throwsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAll() — returns paginated list")
    void getAll_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(List.of(project));
        when(projectRepository.findAll(pageable)).thenReturn(page);

        Page<ProjectResponse> result = projectService.getAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("update() — updates and returns modified project when name unchanged")
    void update_success() {
        // name is unchanged ("Test Project" == "Test Project"), so existsByName is NOT called
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        request.setDescription("Updated description");

        ProjectResponse response = projectService.update(1L, request);

        assertThat(response.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("update() — throws ConflictException when new name already taken")
    void update_newNameConflict_throwsConflict() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.existsByName("Other Project")).thenReturn(true);

        request.setName("Other Project");

        assertThatThrownBy(() -> projectService.update(1L, request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("delete() — removes project")
    void delete_success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.delete(1L);

        verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("create() — assigns team when teamId is provided")
    void create_withTeam_assignsTeam() {
        Team team = new Team();
        team.setId(1L);
        team.setName("Platform Engineering");

        request.setTeamId(1L);

        when(projectRepository.existsByName(anyString())).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectResponse response = projectService.create(request);

        assertThat(response.getTeamId()).isEqualTo(1L);
        assertThat(response.getTeamName()).isEqualTo("Platform Engineering");
    }
}
