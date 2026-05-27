# Anomaly detection

CloudFlow watches each service's error-rate and records an anomaly when it
spikes, so the ops-assistant can answer "what anomalies happened today?" and
"why did orders error-rate spike at 14:00?" with grounded, time-windowed
evidence.

## How it works

The detector (`platform-common`, `AnomalyDetector`) buckets a service's logs into
fixed windows (1 minute by default). For each window it computes the error
fraction and feeds it through an exponentially-weighted moving average and
variance:

```
mean_t  = mean_{t-1} + alpha * (rate_t - mean_{t-1})
var_t   = (1 - alpha) * (var_{t-1} + alpha * (rate_t - mean_{t-1})^2)
z_t     = (rate_t - mean_t) / sqrt(var_t)
```

A window fires an anomaly when, after a short warmup, its z-score exceeds the
threshold (3 by default) while the rate is above the running mean. When the
baseline is perfectly flat (zero variance) the z-score is undefined, so a large
absolute jump above the mean fires instead. Each anomaly carries its window
bounds, the observed error-rate, the z-score, and the top error signatures seen
in the window.

## Flow

1. The `log-collector` ingests structured logs into the shared store.
2. After each ingest batch (and on demand via `POST /v1/ingest/detect`) the
   collector runs the detector and writes anomalies to the `anomaly` table.
3. The `ops-assistant` reads that table. An anomaly question is routed to the
   anomaly path rather than general RAG:
   - "what anomalies happened today?" lists today's anomalies and cites their
     `anomaly:<id>`.
   - "why did X spike at HH:MM?" pulls the logs in that one-minute window and
     cites each `log:<id>`.

## Tested invariant

The detector test injects ten calm minutes followed by a one-minute error spike
and asserts a single anomaly fires for the right window with the right signature.
The assistant test then asks "why did orders error-rate spike at 14:00?" and
asserts the answer cites a log line from that window. Citations stay grounded:
the assistant only cites anomaly and log ids that exist in the store.
