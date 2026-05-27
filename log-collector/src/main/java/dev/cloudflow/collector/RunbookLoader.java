package dev.cloudflow.collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Loads runbook markdown from disk into the RAG store once the app is ready. */
@Component
public class RunbookLoader {

  private final IngestService ingestService;
  private final String runbooksDir;

  public RunbookLoader(
      IngestService ingestService, @Value("${cloudflow.runbooks.dir:docs/runbooks}") String dir) {
    this.ingestService = ingestService;
    this.runbooksDir = dir;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void loadOnStartup() {
    load(Path.of(runbooksDir));
  }

  /** Ingests every {@code *.md} file in the directory. Returns the number of chunks stored. */
  public int load(Path dir) {
    if (!Files.isDirectory(dir)) {
      return 0;
    }
    int total = 0;
    try (Stream<Path> files = Files.list(dir)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".md")).toList()) {
        String slug = file.getFileName().toString().replaceFirst("\\.md$", "");
        String markdown = Files.readString(file);
        total += ingestService.ingestRunbook(slug, markdown);
      }
    } catch (IOException e) {
      throw new IllegalStateException("could not load runbooks from " + dir, e);
    }
    return total;
  }
}
