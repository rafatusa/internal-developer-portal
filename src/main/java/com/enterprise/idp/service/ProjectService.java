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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Project CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    /** Create a new project. */
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByName(request.getName())) {
            throw new ConflictException("Project name '" + request.getName() + "' already exists");
        }
        Project project = Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .status(request.getStatus() != null ? request.getStatus() : ProjectStatus.ACTIVE)
            .repoUrl(request.getRepoUrl())
            .techStack(request.getTechStack())
            .build();

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));
            project.setTeam(team);
        }
        return toResponse(projectRepository.save(project));
    }

    /** Get project by ID. */
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return toResponse(findById(id));
    }

    /** List all projects paginated. */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(this::toResponse);
    }

    /** Search projects by name or description. */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> search(String query, Pageable pageable) {
        return projectRepository.search(query, pageable).map(this::toResponse);
    }

    /** Filter projects by status. */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getByStatus(ProjectStatus status, Pageable pageable) {
        return projectRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    /** Update an existing project. */
    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findById(id);

        if (!project.getName().equals(request.getName())
            && projectRepository.existsByName(request.getName())) {
            throw new ConflictException("Project name '" + request.getName() + "' already exists");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus() != null ? request.getStatus() : project.getStatus());
        project.setRepoUrl(request.getRepoUrl());
        project.setTechStack(request.getTechStack());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));
            project.setTeam(team);
        }
        return toResponse(projectRepository.save(project));
    }

    /** Delete a project. */
    @Transactional
    public void delete(Long id) {
        Project project = findById(id);
        projectRepository.delete(project);
        log.info("Project '{}' (id={}) deleted", project.getName(), id);
    }

    private Project findById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse resp = new ProjectResponse();
        resp.setId(project.getId());
        resp.setName(project.getName());
        resp.setDescription(project.getDescription());
        resp.setStatus(project.getStatus());
        resp.setRepoUrl(project.getRepoUrl());
        resp.setTechStack(project.getTechStack());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        if (project.getTeam() != null) {
            resp.setTeamId(project.getTeam().getId());
            resp.setTeamName(project.getTeam().getName());
        }
        return resp;
    }
}
