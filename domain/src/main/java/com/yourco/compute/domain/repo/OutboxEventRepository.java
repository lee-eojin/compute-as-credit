package com.yourco.compute.domain.repo;

import com.yourco.compute.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();
}
