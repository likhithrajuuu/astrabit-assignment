package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByGuildIdOrderByPriorityAsc(String guildId);
}
