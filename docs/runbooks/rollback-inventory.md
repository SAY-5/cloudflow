# Runbook: Roll back the inventory service

Use this when a bad inventory release is serving errors and you need to return
to the previous known-good revision.

## Symptoms

- Inventory error-rate spiked right after a deploy.
- `/actuator/health` for inventory reports `DOWN` or 5xx responses climb.

## Steps

1. Confirm the current Helm revision:

   ```
   helm history inventory
   ```

2. Roll back to the previous revision:

   ```
   helm rollback inventory
   ```

3. Verify the rollout recovered:

   ```
   kubectl rollout status deployment/inventory
   ```

4. Re-check the health endpoint:

   ```
   kubectl get pods -l app=inventory
   ```

## Verify before running

Confirm you are pointed at the right cluster and namespace before issuing any
command. A rollback restarts pods, so expect a brief drop in capacity.
