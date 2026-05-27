package dev.cloudflow.assistant.store;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RagChunkRepository extends JpaRepository<RagChunkEntity, String> {

  List<RagChunkEntity> findBySource(String source);

  @Query(
      "select c from RagChunkEntity c where c.source = 'log' and c.service = :service "
          + "and c.ts >= :from and c.ts < :to")
  List<RagChunkEntity> findLogsInWindow(
      @Param("service") String service, @Param("from") Instant from, @Param("to") Instant to);
}
