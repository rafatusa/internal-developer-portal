package com.example.internaldeveloperportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.internaldeveloperportal.domain.Team;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamRequest;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamResponse;
import com.example.internaldeveloperportal.exception.ConflictException;
import com.example.internaldeveloperportal.exception.ResourceNotFoundException;
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
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    private static Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    @Test
    @DisplayName("findAll maps every entity to a response")
    void findAllMaps() {
        when(teamRepository.findAll()).thenReturn(List.of(team(1L, "platform"), team(2L, "data")));

        List<TeamResponse> result = teamService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TeamResponse::name).containsExactly("platform", "data");
    }

    @Test
    @DisplayName("findById returns the mapped team")
    void findByIdMaps() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "platform")));

        assertThat(teamService.findById(1L).name()).isEqualTo("platform");
    }

    @Test
    @DisplayName("findById raises not-found for an unknown id")
    void findByIdMissing() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create persists a new team")
    void createPersists() {
        TeamRequest request = new TeamRequest("platform", "Platform team", "platform@example.com");
        when(teamRepository.existsByName("platform")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(call -> {
            Team saved = call.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        TeamResponse created = teamService.create(request);

        assertThat(created.id()).isEqualTo(7L);
        assertThat(created.contactEmail()).isEqualTo("platform@example.com");
    }

    @Test
    @DisplayName("create rejects a duplicate name")
    void createRejectsDuplicate() {
        when(teamRepository.existsByName("platform")).thenReturn(true);

        assertThatThrownBy(() ->
            teamService.create(new TeamRequest("platform", null, null)))
            .isInstanceOf(ConflictException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("update keeps the same name without a uniqueness check failure")
    void updateSameName() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "platform")));
        when(teamRepository.save(any(Team.class))).thenAnswer(call -> call.getArgument(0));

        TeamResponse updated =
            teamService.update(1L, new TeamRequest("platform", "renamed desc", null));

        assertThat(updated.description()).isEqualTo("renamed desc");
    }

    @Test
    @DisplayName("update rejects renaming onto an existing team")
    void updateRejectsRenameCollision() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "platform")));
        when(teamRepository.existsByName("data")).thenReturn(true);

        assertThatThrownBy(() -> teamService.update(1L, new TeamRequest("data", null, null)))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("delete removes an existing team")
    void deleteRemoves() {
        Team existing = team(1L, "platform");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));

        teamService.delete(1L);

        verify(teamRepository).delete(existing);
    }
}
