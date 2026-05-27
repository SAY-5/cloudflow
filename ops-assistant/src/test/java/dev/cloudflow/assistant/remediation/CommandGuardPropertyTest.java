package dev.cloudflow.assistant.remediation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class CommandGuardPropertyTest {

  @Provide
  Arbitrary<String> commands() {
    Arbitrary<String> tool = Arbitraries.of("kubectl", "helm");
    Arbitrary<String> verb =
        Arbitraries.of("get", "rollout", "rollback", "delete", "drain", "scale", "status");
    Arbitrary<String> arg = Arbitraries.of("inventory", "deployment/orders", "pod", "-l app=x");
    return net.jqwik.api.Combinators.combine(tool, verb, arg)
        .as((t, v, a) -> t + " " + v + " " + a);
  }

  @Property(tries = 500)
  void allowedNeverContainsADestructiveCommandAbsentFromRunbook(
      @ForAll("commands") String command) {
    String runbookWithoutCommands = "This runbook contains only prose and no shell commands.";

    List<String> allowed = CommandGuard.allowed(List.of(command), runbookWithoutCommands);

    assertThat(allowed).allSatisfy(c -> assertThat(CommandGuard.isDestructive(c)).isFalse());
  }

  @Property(tries = 500)
  void wellShapedDestructiveCommandPassesOnlyWhenRunbookContainsIt(
      @ForAll("commands") String command) {
    boolean destructive = CommandGuard.isDestructive(command);
    String runbookWithCommand = "Step: " + command + " to recover.";

    List<String> allowed = CommandGuard.allowed(List.of(command), runbookWithCommand);

    // Whether destructive or not, a well-shaped command present in the runbook is allowed.
    assertThat(allowed).containsExactly(command);
    if (!destructive) {
      // Non-destructive commands are allowed regardless of runbook contents.
      assertThat(CommandGuard.allowed(List.of(command), "unrelated")).containsExactly(command);
    }
  }
}
