package dev.cloudflow.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RunbookLoaderTest {

  @Autowired private IngestService ingestService;
  @Autowired private RagChunkRepository repository;

  @Test
  void loadsEveryMarkdownFileInTheDirectory(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("rollback.md"), "# Rollback\n\nrun helm rollback inventory\n");
    Files.writeString(dir.resolve("spike.md"), "# Spike\n\ngroup logs by signature\n");

    RunbookLoader loader = new RunbookLoader(ingestService, dir.toString());
    int chunks = loader.load(dir);

    assertThat(chunks).isGreaterThanOrEqualTo(2);
    assertThat(repository.findBySource("doc")).isNotEmpty();
  }

  @Test
  void missingDirectoryLoadsNothing(@TempDir Path dir) {
    RunbookLoader loader = new RunbookLoader(ingestService, dir.toString());
    assertThat(loader.load(dir.resolve("does-not-exist"))).isZero();
  }
}
