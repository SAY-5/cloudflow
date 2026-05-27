package dev.cloudflow.assistant.store;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

  @Query(
      "select a from AnomalyEntity a where a.windowStart >= :from and a.windowStart < :to "
          + "order by a.windowStart")
  List<AnomalyEntity> findInRange(@Param("from") Instant from, @Param("to") Instant to);
}
