import { useQuery } from "@tanstack/react-query";
import { fetchHealth, type HealthStatus } from "../api";

const STATUS_COLOR: Record<HealthStatus, string> = {
  UP: "#1f9d55",
  DEGRADED: "#d97706",
  DOWN: "#dc2626",
};

export function HealthGrid() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["health"],
    queryFn: fetchHealth,
    refetchInterval: 5000,
  });

  if (isLoading) return <p>Loading platform health...</p>;
  if (isError || !data) return <p role="alert">Could not load platform health.</p>;

  return (
    <section aria-labelledby="health-heading">
      <h2 id="health-heading">
        Platform health:{" "}
        <span style={{ color: STATUS_COLOR[data.status] }}>{data.status}</span>
      </h2>
      <ul className="health-grid">
        {data.services.map((svc) => (
          <li key={svc.service} className="health-card">
            <span
              className="dot"
              style={{ background: svc.up ? STATUS_COLOR.UP : STATUS_COLOR.DOWN }}
              aria-hidden="true"
            />
            <strong>{svc.service}</strong>
            <span className="detail">{svc.up ? "UP" : svc.detail}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
