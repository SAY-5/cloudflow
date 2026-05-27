import { useQuery } from "@tanstack/react-query";
import { fetchLogs } from "../api";

const LEVEL_COLOR: Record<string, string> = {
  ERROR: "#dc2626",
  WARN: "#d97706",
  INFO: "#2563eb",
};

export function LogViewer() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["logs"],
    queryFn: fetchLogs,
    refetchInterval: 5000,
  });

  if (isLoading) return <p>Loading logs...</p>;
  if (isError || !data) return <p role="alert">Could not load logs.</p>;

  return (
    <section aria-labelledby="logs-heading">
      <h2 id="logs-heading">Recent logs</h2>
      <table className="log-table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Service</th>
            <th>Level</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          {data.map((line) => (
            <tr key={line.id}>
              <td>{line.ts}</td>
              <td>{line.service}</td>
              <td style={{ color: LEVEL_COLOR[line.level] ?? "inherit" }}>{line.level}</td>
              <td>{line.msg}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
