package com.enterprise.idp.service;

import com.enterprise.idp.domain.team.Team;
import com.enterprise.idp.domain.team.TeamRepository;
import com.enterprise.idp.dto.team.TeamRequest;
import com.enterprise.idp.dto.team.TeamResponse;
import com.enterprise.idp.exception.ConflictException;
import com.enterprise.idp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Team CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    /** Create a new team. */
    @Transactional
    public TeamResponse create(TeamRequest request) {
        if (teamRepository.existsByName(request.getName())) {
            throw new ConflictException("Team name '" + request.getName() + "' already exists");
        }
        Team team = Team.builder()
            .name(request.getName())
            .description(request.getDescription())
            .slackChannel(request.getSlackChannel())
            .emailDistribution(request.getEmailDistribution())
            .memberCount(request.getMemberCount() != null ? request.getMemberCount() : 0)
            .build();
        return toResponse(teamRepository.save(team));
    }

    /** Get team by ID. */
    @Transactional(readOnly = true)
    public TeamResponse getById(Long id) {
        return toResponse(findById(id));
    }

    /** List all teams paginated. */
    @Transactional(readOnly = true)
    public Page<TeamResponse> getAll(Pageable pageable) {
        return teamRepository.findAll(pageable).map(this::toResponse);
    }

    /** Search teams by name. */
    @Transactional(readOnly = true)
    public Page<TeamResponse> search(String query, Pageable pageable) {
        return teamRepository.search(query, pageable).map(this::toResponse);
    }

    /** Update a team. */
    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = findById(id);
        if (!team.getName().equals(request.getName())
            && teamRepository.existsByName(request.getName())) {
            throw new ConflictException("Team name '" + request.getName() + "' already exists");
        }
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setSlackChannel(request.getSlackChannel());
        team.setEmailDistribution(request.getEmailDistribution());
        if (request.getMemberCount() != null) {
            team.setMemberCount(request.getMemberCount());
        }
        return toResponse(teamRepository.save(team));
    }

    /** Delete a team. */
    @Transactional
    public void delete(Long id) {
        Team team = findById(id);
        teamRepository.delete(team);
        log.info("Team '{}' (id={}) deleted", team.getName(), id);
    }

    private Team findById(Long id) {
        return teamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    private TeamResponse toResponse(Team team) {
        TeamResponse resp = new TeamResponse();
        resp.setId(team.getId());
        resp.setName(team.getName());
        resp.setDescription(team.getDescription());
        resp.setSlackChannel(team.getSlackChannel());
        resp.setEmailDistribution(team.getEmailDistribution());
        resp.setMemberCount(team.getMemberCount());
        resp.setCreatedAt(team.getCreatedAt());
        resp.setUpdatedAt(team.getUpdatedAt());
        return resp;
    }
}
