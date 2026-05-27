package dev.cloudflow.assistant.remediation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cloudflow.assistant.remediation.CommandGuard.BlockedCommandException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandGuardTest {

  @Test
  void acceptsWellShapedNonDestructiveCommands() {
    assertThat(CommandGuard.isWellShaped("helm rollback inventory")).isTrue();
    assertThat(CommandGuard.isWellShaped("kubectl rollout status deployment/inventory")).isTrue();
    CommandGuard.verify("helm rollback inventory", "the runbook body");
  }

  @Test
  void rejectsNonKubectlHelmText() {
    assertThat(CommandGuard.isWellShaped("rm -rf /")).isFalse();
    assertThatThrownBy(() -> CommandGuard.verify("rm -rf /", "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blocksDestructiveCommandNotInRunbook() {
    assertThat(CommandGuard.isDestructive("kubectl delete deployment/orders")).isTrue();
    assertThatThrownBy(
            () -> CommandGuard.verify("kubectl delete deployment/orders", "a safe runbook"))
        .isInstanceOf(BlockedCommandException.class);
  }

  @Test
  void allowsDestructiveCommandWhenRunbookContainsItVerbatim() {
    String runbook = "If pods are wedged, run kubectl delete pod -l app=orders to restart them.";
    CommandGuard.verify("kubectl delete pod -l app=orders", runbook);
  }

  @Test
  void filtersListToAllowedCommands() {
    String runbook = "run helm rollback inventory then kubectl rollout status deployment/inventory";
    List<String> result =
        CommandGuard.allowed(
            List.of(
                "helm rollback inventory",
                "kubectl rollout status deployment/inventory",
                "kubectl delete namespace prod"),
            runbook);
    assertThat(result)
        .containsExactly("helm rollback inventory", "kubectl rollout status deployment/inventory");
  }
}
