package dev.cloudflow.collector;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {}
