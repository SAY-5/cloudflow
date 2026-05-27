package dev.cloudflow.collector;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Ingest endpoints for structured logs. */
@RestController
@RequestMapping("/v1/ingest")
public class CollectorController {

  private final IngestService ingestService;

  public CollectorController(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  public record IngestLogsRequest(List<String> lines) {}

  public record IngestResult(int stored) {}

  @PostMapping("/logs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public IngestResult ingestLogs(@RequestBody IngestLogsRequest request) {
    return new IngestResult(ingestService.ingestLogLines(request.lines()));
  }
}
