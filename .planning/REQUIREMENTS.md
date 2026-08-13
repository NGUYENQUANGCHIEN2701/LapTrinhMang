# Requirements: Tank Battle Online

**Defined:** 2026-08-13
**Core Value:** Two players can play a real-time tank battle against each other over the network, end-to-end — client and server actually communicating live game state, not a local single-player mockup.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [ ] **AUTH-01**: User can register a new account with a username and password
- [ ] **AUTH-02**: User can log in with their account
- [ ] **AUTH-03**: User's login session persists across a page refresh

### Matchmaking

- [ ] **MATCH-01**: Two logged-in users can be paired into a 1v1 match (simple queue or room join — no skill-based ranking)
- [ ] **MATCH-02**: User sees a waiting/matched status while queued for an opponent

### Combat

- [ ] **COMBAT-01**: Player can move their tank in real time; movement is visible live to the opponent
- [ ] **COMBAT-02**: Player can fire; bullets travel and hit detection is resolved by the server
- [ ] **COMBAT-03**: Tank has HP; taking a hit reduces HP
- [ ] **COMBAT-04**: Match ends with a clear winner when a tank's HP reaches 0
- [ ] **COMBAT-05**: Map has walls/obstacles that block tank movement and stop bullets
- [ ] **COMBAT-06**: Bullets ricochet off walls (limited bounce count) instead of simply stopping

### Match Lifecycle

- [ ] **LIFE-01**: If a player disconnects mid-match, the match ends and the remaining player is declared the winner
- [ ] **LIFE-02**: Player sees a clear win/lose result screen when a match ends

### Chat

- [ ] **CHAT-01**: Players can send text chat messages to each other during a match

### Leaderboard

- [ ] **LEADER-01**: Player can view a leaderboard showing win/loss records across matches

### Practice

- [ ] **BOT-01**: Player can practice against an AI-controlled tank when a second human player isn't available

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Polish

- **POLISH-01**: Power-ups on the map (speed boost, shield, extra damage, heal)
- **POLISH-02**: Multiple selectable tank types with different stats
- **POLISH-03**: Multiple selectable maps
- **POLISH-04**: Sound effects and visual polish (explosions, muzzle flash, screen shake)

### Stats

- **STATS-01**: Match history / per-match stats (shots fired, accuracy, damage dealt)

### Reliability

- **REL-01**: Reconnect into an in-progress match after accidental disconnect

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| More than 2 players per match (3v3, battle royale) | State-sync complexity scales non-linearly with player count — beyond a solo, several-week course project |
| Skill-based matchmaking / ELO rating | No meaningful player pool for a course demo; win/loss counts on the leaderboard are sufficient |
| Spectator mode / match replay | Requires a second delivery pipeline (broadcast or state-log recording) on top of the live game |
| Native mobile app | Different rendering/input stack and packaging; doesn't validate the networking coursework — web-first only |
| Tank progression / upgrade trees, persistent currency, in-game shop | Turns the project into an economy-design problem unrelated to the networking learning objective |
| Voice chat | Requires a separate WebRTC/audio subsystem for marginal value over text chat |
| Full client-side prediction + server reconciliation + lag compensation (rollback netcode) | Overkill complexity-to-payoff ratio for a low-latency 1v1 course demo; a simple server-authoritative loop is sufficient |
| Anti-cheat systems | No economy or real stakes to protect; server-authoritative state is itself the baseline defense |
| Custom map editor | A full editor UI is a second application; 1-3 hardcoded map layouts satisfy "map with walls" |
| Global lobby chat, friends list, DMs | Expands chat into a persistent social system with no bearing on the core 1v1 loop |
| Achievements / badges | Pure scope addition with no relationship to the networking objective |
| Raw hand-rolled TCP/UDP byte-level protocol | Not required by the course; a Java WebSocket-based server satisfies the client-server networking requirement |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | TBD | Pending |
| AUTH-02 | TBD | Pending |
| AUTH-03 | TBD | Pending |
| MATCH-01 | TBD | Pending |
| MATCH-02 | TBD | Pending |
| COMBAT-01 | TBD | Pending |
| COMBAT-02 | TBD | Pending |
| COMBAT-03 | TBD | Pending |
| COMBAT-04 | TBD | Pending |
| COMBAT-05 | TBD | Pending |
| COMBAT-06 | TBD | Pending |
| LIFE-01 | TBD | Pending |
| LIFE-02 | TBD | Pending |
| CHAT-01 | TBD | Pending |
| LEADER-01 | TBD | Pending |
| BOT-01 | TBD | Pending |

**Coverage:**
- v1 requirements: 16 total
- Mapped to phases: 0 (roadmap not yet created)
- Unmapped: 16 ⚠️

---
*Requirements defined: 2026-08-13*
*Last updated: 2026-08-13 after initial definition*
