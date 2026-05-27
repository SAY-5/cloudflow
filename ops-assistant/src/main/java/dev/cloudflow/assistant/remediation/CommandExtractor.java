package dev.cloudflow.assistant.remediation;

import java.util.ArrayList;
import java.util.List;

/** Extracts candidate shell command lines from runbook markdown. */
public final class CommandExtractor {

  private CommandExtractor() {}

  /**
   * Returns the kubectl/helm command lines found in the markdown, in order. Commands inside fenced
   * code blocks and indented blocks are both considered.
   */
  public static List<String> extract(String markdown) {
    List<String> commands = new ArrayList<>();
    if (markdown == null) {
      return commands;
    }
    for (String raw : markdown.split("\n")) {
      String line = raw.strip();
      if (line.startsWith("kubectl ") || line.startsWith("helm ")) {
        if (CommandGuard.isWellShaped(line) && !commands.contains(line)) {
          commands.add(line);
        }
      }
    }
    return commands;
  }
}
