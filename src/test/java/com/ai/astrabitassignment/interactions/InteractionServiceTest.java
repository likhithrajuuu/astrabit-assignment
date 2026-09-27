package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.entities.Interaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    InteractionRepository repository;

    @Mock
    EntityManager entityManager;

    @Mock
    Query query;

    InteractionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new InteractionService(repository, JsonMapper.builder().build());
        Field emField = InteractionService.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(service, entityManager);
    }

    private Map<String, Object> interactionPayload(String discordInteractionId) {
        return Map.of(
                "id", discordInteractionId,
                "type", 2,
                "guild_id", "guild-1",
                "data", Map.of("name", "ping"),
                "member", Map.of("user", Map.of("id", "user-1", "username", "tester"))
        );
    }

    @Test
    void savesAGenuinelyNewInteraction() {
        when(repository.existsByDiscordInteractionId("interaction-1")).thenReturn(false);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(42L);
        when(repository.save(any(Interaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Interaction> result = service.saveIfNew(interactionPayload("interaction-1"));

        assertThat(result).isPresent();
        assertThat(result.get().getDiscordInteractionId()).isEqualTo("interaction-1");
        assertThat(result.get().getStatus()).isEqualTo("RECEIVED");
        verify(repository).save(any(Interaction.class));
    }

    @Test
    void skipsARedeliveredInteractionWithoutTouchingProvisioningOrSave() {
        when(repository.existsByDiscordInteractionId("interaction-1")).thenReturn(true);

        Optional<Interaction> result = service.saveIfNew(interactionPayload("interaction-1"));

        assertThat(result).isEmpty();
        verifyNoInteractions(entityManager);
        verify(repository, never()).save(any());
    }

    @Test
    void treatsALostRaceOnTheUniqueConstraintAsADuplicateNotAnError() {
        when(repository.existsByDiscordInteractionId("interaction-1")).thenReturn(false);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(42L);
        when(repository.save(any(Interaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        Optional<Interaction> result = service.saveIfNew(interactionPayload("interaction-1"));

        assertThat(result).isEmpty();
    }
}
