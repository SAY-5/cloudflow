export type HealthStatus = "UP" | "DEGRADED" | "DOWN";

export interface ServiceHealth {
  service: string;
  up: boolean;
  detail: string;
}

export interface PlatformHealth {
  status: HealthStatus;
  services: ServiceHealth[];
}

export interface LogLine {
  id: string;
  service: string;
  level: string;
  msg: string;
  ts: string;
}

export interface Citation {
  id: string;
  source: string;
  snippet: string;
}

export interface AssistResult {
  answer: string;
  citations: Citation[];
}

const API_BASE = "/api";

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(`request failed: ${res.status}`);
  }
  return (await res.json()) as T;
}

export async function fetchHealth(): Promise<PlatformHealth> {
  return json<PlatformHealth>(await fetch(`${API_BASE}/health`));
}

export async function fetchLogs(): Promise<LogLine[]> {
  return json<LogLine[]>(await fetch(`${API_BASE}/logs`));
}

export async function askAssistant(question: string): Promise<AssistResult> {
  const res = await fetch(`${API_BASE}/assist`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });
  return json<AssistResult>(res);
}
