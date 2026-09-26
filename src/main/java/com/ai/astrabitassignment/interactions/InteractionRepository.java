package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.entities.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
}
