package dev.cloudflow.assistant.remediation;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates that a suggested command is a syntactically-shaped {@code kubectl} or {@code helm}
 * invocation, and blocks destructive verbs unless the source runbook explicitly contains them.
 *
 * <p>Commands are never executed; this is a static safety check on the text the assistant proposes.
 */
public final class CommandGuard {

  private static final Pattern SHAPE =
      Pattern.compile("^(kubectl|helm)\\s+[a-z][a-z0-9-]*(\\s+\\S+)*$");

  private static final Set<String> DESTRUCTIVE =
      Set.of("delete", "drain", "uncordon", "cordon", "destroy");

  private CommandGuard() {}

  /** Thrown when a destructive command is suggested that the runbook does not contain. */
  public static final class BlockedCommandException extends RuntimeException {
    public BlockedCommandException(String message) {
      super(message);
    }
  }

  /** True when the command looks like a kubectl/helm invocation. */
  public static boolean isWellShaped(String command) {
    return command != null && SHAPE.matcher(command.strip()).matches();
  }

  /** True when the command's verb is one of the destructive set. */
  public static boolean isDestructive(String command) {
    String[] parts = command.strip().split("\\s+");
    if (parts.length < 2) {
      return false;
    }
    return DESTRUCTIVE.contains(parts[1].toLowerCase(Locale.ROOT));
  }

  /**
   * Verifies a suggested command. A well-shaped non-destructive command passes. A destructive
   * command passes only if {@code runbookText} contains it verbatim; otherwise it is blocked.
   *
   * @throws IllegalArgumentException if the command is not a kubectl/helm shape
   * @throws BlockedCommandException if a destructive command is not present in the runbook
   */
  public static void verify(String command, String runbookText) {
    String trimmed = command.strip();
    if (!isWellShaped(trimmed)) {
      throw new IllegalArgumentException("not a kubectl/helm command: " + command);
    }
    if (isDestructive(trimmed) && !containsCommand(runbookText, trimmed)) {
      throw new BlockedCommandException(
          "destructive command blocked (not present in runbook): " + trimmed);
    }
  }

  /** Filters a list of candidate commands to the ones that pass the guard against the runbook. */
  public static List<String> allowed(List<String> commands, String runbookText) {
    return commands.stream()
        .filter(CommandGuard::isWellShaped)
        .filter(c -> !isDestructive(c) || containsCommand(runbookText, c))
        .toList();
  }

  private static boolean containsCommand(String runbookText, String command) {
    if (runbookText == null) {
      return false;
    }
    return runbookText.contains(command.strip());
  }
}
