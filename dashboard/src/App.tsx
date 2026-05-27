import { HealthGrid } from "./components/HealthGrid";
import { LogViewer } from "./components/LogViewer";
import { AssistantPanel } from "./components/AssistantPanel";
import "./styles.css";

export function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>CloudFlow Ops</h1>
        <p>Hybrid-cloud microservice platform console</p>
      </header>
      <main className="app-main">
        <HealthGrid />
        <AssistantPanel />
        <LogViewer />
      </main>
    </div>
  );
}
