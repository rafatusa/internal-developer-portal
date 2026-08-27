package com.example.internaldeveloperportal.service;

import com.example.internaldeveloperportal.domain.Team;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamRequest;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamResponse;
import com.example.internaldeveloperportal.exception.ConflictException;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
import com.example.internaldeveloperportal.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD operations for teams. */
@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream().map(TeamResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse findById(Long id) {
        return TeamResponse.from(require(id));
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new ConflictException("Team '" + request.name() + "' already exists");
        }
        Team team = new Team();
        apply(team, request);
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = require(id);
        if (!team.getName().equals(request.name()) && teamRepository.existsByName(request.name())) {
            throw new ConflictException("Team '" + request.name() + "' already exists");
        }
        apply(team, request);
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        teamRepository.delete(require(id));
    }

    private Team require(Long id) {
        return teamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    private void apply(Team team, TeamRequest request) {
        team.setName(request.name());
        team.setDescription(request.description());
        team.setContactEmail(request.contactEmail());
    }
}
