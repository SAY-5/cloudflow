# Runbook auto-suggestion

When an operator asks "how do I fix X?" or "how do I roll back X?", the
ops-assistant goes past a plain answer and returns a structured remediation drawn
from the matching runbook and the recent anomaly context.

## What it returns

`POST /v1/remediate {question}` returns:

- a `summary` naming the runbook it drew from,
- ordered `steps` taken from the runbook's numbered list,
- the `commands` (kubectl/helm) extracted from the runbook that passed the
  guardrail,
- any `blockedCommands` the guardrail rejected,
- a `confidence` in `[0,1]` that rises with the number of steps and commands
  found and whether recent anomalies are recorded,
- an explicit `disclaimer` to verify before running,
- the `citations` (the runbook chunk ids) it relied on.

The assistant never executes anything; the commands are suggestions.

## The guardrail

`CommandGuard` enforces two rules on every suggested command:

1. It must be a syntactically-shaped `kubectl` or `helm` invocation. Anything
   else (for example `rm -rf /`) is rejected outright.
2. A destructive verb (`delete`, `drain`, `cordon`, `uncordon`, `destroy`) is
   blocked unless the source runbook contains that exact command verbatim. This
   keeps the assistant from inventing a destructive step that the runbook author
   never sanctioned, while still surfacing destructive commands an author chose
   to document.

## Tested invariant

The remediation test asks "how do I roll back inventory?" and asserts the
suggestion contains the runbook's `helm rollback inventory` and
`kubectl rollout status deployment/inventory` steps, cites the
`doc:rollback-inventory` chunks, and that the guardrail passes for every
suggested command. A property test asserts the allowed set never contains a
destructive command that is absent from the runbook.
