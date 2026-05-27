package dev.cloudflow.collector;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Ingest endpoints for structured logs. */
@RestController
@RequestMapping("/v1/ingest")
public class CollectorController {

  private final IngestService ingestService;
  private final RagChunkRepository repository;

  public CollectorController(IngestService ingestService, RagChunkRepository repository) {
    this.ingestService = ingestService;
    this.repository = repository;
  }

  public record IngestLogsRequest(List<String> lines) {}

  public record IngestResult(int stored) {}

  public record LogLineView(String id, String service, String level, String msg, String ts) {}

  @PostMapping("/logs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public IngestResult ingestLogs(@RequestBody IngestLogsRequest request) {
    return new IngestResult(ingestService.ingestLogLines(request.lines()));
  }

  /** Returns the most recent stored log lines for the dashboard log viewer. */
  @GetMapping("/logs")
  public List<LogLineView> recentLogs(@RequestParam(defaultValue = "50") int limit) {
    return repository.findBySource("log").stream()
        .sorted(
            Comparator.comparing(
                RagChunkEntity::getTs, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(limit)
        .map(CollectorController::toView)
        .toList();
  }

  private static LogLineView toView(RagChunkEntity e) {
    String content = e.getContent();
    String[] parts = content.split(" ", 3);
    String level = parts.length > 1 ? parts[1] : "INFO";
    String msg = parts.length > 2 ? parts[2] : content;
    return new LogLineView(
        e.getId(), e.getService(), level, msg, e.getTs() == null ? "" : e.getTs().toString());
  }
}
