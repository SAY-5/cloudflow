# Runbook: Investigate an orders error-rate spike

Use this when the orders service shows a sudden rise in error responses.

## Symptoms

- Orders error-rate climbs above its normal baseline.
- Logs show repeated `payment gateway timeout` or `502` signatures.

## Steps

1. Identify the time window of the spike from the anomaly store or the log
   viewer.

2. Pull the orders error logs for that window and group by signature:

   ```
   kubectl logs -l app=orders --since=15m
   ```

3. If a downstream dependency is the cause, scale orders to shed load:

   ```
   kubectl scale deployment/orders --replicas=4
   ```

4. If a recent deploy is the cause, follow the rollback runbook.

## Verify before running

Scaling changes resource usage. Confirm cluster headroom before scaling up.
