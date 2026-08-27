package com.example.internaldeveloperportal.service;

import com.example.internaldeveloperportal.domain.Project;
import com.example.internaldeveloperportal.domain.Team;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectRequest;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectResponse;
import com.example.internaldeveloperportal.exception.ConflictException;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.ProjectRepository;
import com.example.internaldeveloperportal.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD operations for projects. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    public ProjectService(ProjectRepository projectRepository, TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(Long teamId) {
        List<Project> projects = teamId == null
            ? projectRepository.findAll()
            : projectRepository.findByTeamId(teamId);
        return projects.stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(require(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByName(request.name())) {
            throw new ConflictException("Project '" + request.name() + "' already exists");
        }
        Project project = new Project();
        apply(project, request);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = require(id);
        if (!project.getName().equals(request.name())
                && projectRepository.existsByName(request.name())) {
            throw new ConflictException("Project '" + request.name() + "' already exists");
        }
        apply(project, request);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(require(id));
    }

    private Project require(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private void apply(Project project, ProjectRequest request) {
        project.setName(request.name());
        project.setDescription(request.description());
        project.setRepositoryUrl(request.repositoryUrl());
        project.setLanguage(request.language());
        if (request.teamId() == null) {
            project.setTeam(null);
        } else {
            Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.teamId()));
            project.setTeam(team);
        }
    }
}
