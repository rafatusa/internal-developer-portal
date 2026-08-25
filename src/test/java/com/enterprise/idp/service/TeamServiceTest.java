package com.enterprise.idp.service;

import com.enterprise.idp.domain.team.Team;
import com.enterprise.idp.domain.team.TeamRepository;
import com.enterprise.idp.dto.team.TeamRequest;
import com.enterprise.idp.dto.team.TeamResponse;
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
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    private Team team;
    private TeamRequest request;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);
        team.setName("Platform Engineering");
        team.setDescription("Core platform team");
        team.setEmailDistribution("platform@enterprise.com");
        team.setSlackChannel("#platform-eng");

        request = new TeamRequest();
        request.setName("Platform Engineering");
        request.setDescription("Core platform team");
        request.setEmailDistribution("platform@enterprise.com");
        request.setSlackChannel("#platform-eng");
    }

    @Test
    @DisplayName("create() — persists team and returns response")
    void create_success() {
        when(teamRepository.existsByName(anyString())).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        TeamResponse response = teamService.create(request);

        assertThat(response.getName()).isEqualTo("Platform Engineering");
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("create() — throws ConflictException for duplicate name")
    void create_duplicate_throwsConflict() {
        when(teamRepository.existsByName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> teamService.create(request))
            .isInstanceOf(ConflictException.class);
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById() — returns team when found")
    void getById_found() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        TeamResponse response = teamService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Platform Engineering");
    }

    @Test
    @DisplayName("getById() — throws ResourceNotFoundException when not found")
    void getById_notFound() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAll() — returns paginated list of teams")
    void getAll_paginated() {
        when(teamRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(team)));

        Page<TeamResponse> result = teamService.getAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("delete() — removes team by ID")
    void delete_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        teamService.delete(1L);

        verify(teamRepository).delete(team);
    }
}
