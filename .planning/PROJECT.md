# Tank Battle Online

## What This Is

A real-time 1v1 tank battle game for a "Lập trình mạng" (Network Programming) university course. Two players connect to a Java server, control tanks in real time (move, shoot, take damage) on a map with obstacles, and the client is a React web app talking to the Java backend over the network.

## Core Value

Two players can play a real-time tank battle against each other over the network, end-to-end — client and server actually communicating live game state, not a local single-player mockup.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] User can register and log in with an account
- [ ] Two players can be matched into a 1v1 match
- [ ] Player can move their tank and fire in real time; the other player sees it live
- [ ] Tanks have HP; taking hits reduces HP; 0 HP ends the match with a clear winner
- [ ] Map has walls/obstacles that block tank movement and stop bullets
- [ ] Players can send chat messages to each other during a match
- [ ] Player can view a leaderboard of win/loss records across matches

### Out of Scope

- More than 2 players per match — deferred, adds significant state-sync complexity beyond course scope
- Mobile app — web-first, React frontend only
- Skill-based matchmaking/ranking — a simple queue or room join is enough for v1
- Spectator mode / match replay — not needed for grading or core gameplay
- Raw hand-rolled TCP/UDP byte-level protocol — not required by the course; a Java WebSocket-based server is acceptable (see Constraints)

## Context

- This is a solo course project for a Network Programming class. Timeline is several weeks, no hard external deadline yet.
- Original inspiration was a GitHub link (`Tankzors/Builds`) which turned out to be an unrelated Haxe/Kha game (not Java, not reusable code) — it only supplied the "tank battle" gameplay idea, nothing was reused from it.
- Architecture direction: Java backend acts as the server (owns game state, match logic, persistence for accounts/leaderboard); React web app is the client, rendering the game and sending player input over the network.
- The course does not require raw socket-level TCP/UDP programming to be graded — a Java WebSocket server is an acceptable way to satisfy the client-server networking requirement, though hand-rolled sockets could still be used if it adds learning value later.

## Constraints

- **Tech stack**: Java backend, React frontend — user's explicit choice for a client-server architecture with a polished-looking UI
- **Timeline**: Several weeks, solo developer — no fixed external deadline currently known
- **Academic scope**: Deliverable is a gradeable course project, not a production system — favor a working, demonstrable end-to-end flow over polish or scale
- **Architecture**: Java backend design must follow SOLID principles — explicit course/assignment requirement (surfaced via the course's Trello task board, card [G02]); applies to server-side class design (e.g. Match/GameState/connection-handling separation), not the React client

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| React web frontend instead of Java desktop GUI (Swing/JavaFX) | User wants a more polished, modern-looking UI | — Pending |
| Java WebSocket backend instead of mandatory raw TCP/UDP sockets | Course doesn't require raw sockets; user confirmed "not mandatory, just needs to run" | — Pending |
| 1v1 real-time battles only (not turn-based, not >2 players) | Keeps network state-sync scope manageable for a solo, several-week course project | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-13 after adding SOLID architecture constraint*
