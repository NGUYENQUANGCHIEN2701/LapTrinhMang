No external API integration: this phase builds the project's own WebSocket server and browser client; every dependency is a local library compiled or bundled into the app.

---

Javalin, Jackson, React and Vite are libraries, not hosted third-party services, so there is no external capability surface to enumerate or opt out of.

The deterministic detector agrees — run at plan time over the ROADMAP phase scope, `api-coverage.cjs --json` returned `{"detected":false,"signals":[]}`. This declaration is recorded anyway because the plan bodies necessarily use words like "endpoint", "wire" and "connect" about the project's *own* WebSocket route, which could re-trigger the detector at seal time over the wider phase scope.

The first genuine external-service question arrives no earlier than Phase 4 (accounts and persistence), and even there the current stack choice — embedded H2 — keeps it in-process.
