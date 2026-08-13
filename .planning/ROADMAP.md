# Roadmap: Tank Battle Online

## Overview

The journey starts with the riskiest, most novel part of the project — getting a Java server and a React client to agree on live game state over a real network connection — and grows outward from there. Phase 1 delivers a thin but genuinely networked vertical slice (two browsers, two tanks, live movement, server-authoritative). Each later phase thickens that same slice rather than adding a disconnected layer: bullets and walls, then damage and a decisive match end, then real accounts/matchmaking/leaderboard so anonymous test tanks become named players with records, and finally a practice bot and in-match chat that make the game demo-complete solo. Every phase ends with something a grader could watch two browsers do.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Networked Movement Skeleton** - Two browsers connect to the Java server and see each other's tanks move live
- [ ] **Phase 2: Shooting, Walls & Ricochet** - Bullets fire, bounce off walls, and both players see the same shots
- [ ] **Phase 3: Damage, Win Condition & Match Lifecycle** - Hits cost HP, zero HP ends the match, disconnects resolve cleanly
- [ ] **Phase 4: Accounts, Matchmaking & Leaderboard** - Named players log in, get paired automatically, and build a win/loss record
- [ ] **Phase 5: Practice Bot & In-Match Chat** - Playable solo against an AI tank, and players can talk mid-match

## Phase Details

### Phase 1: Networked Movement Skeleton
**Goal**: Two players in separate browsers see each other's tanks move live over a real client-server connection, with the Java server owning the truth
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: COMBAT-01
**Success Criteria** (what must be TRUE):
  1. Two separate browser windows each connect to the running Java server and both render the same arena with both tanks
  2. Pressing a movement key moves your tank on your screen and the other window shows that same movement live, without a refresh
  3. Positions come from the server: a client that sends no input stops moving, and a hand-crafted position message sent from browser devtools does not change what the opponent sees
  4. Both windows still show identical tank positions after both players mash movement keys simultaneously for a sustained stretch (no drift, no server crash)
**Plans**: TBD
**UI hint**: yes

*Notes: This phase locks the three decisions that are expensive to retrofit — message protocol envelope, server-authoritative model, and the concurrency strategy for shared match state (research recommends a single-threaded executor per match). It also locks the fixed-tick server loop (~20-30 Hz) and the client's Canvas render loop. Test with two independent browser processes plus artificial latency, not two tabs on localhost.*

### Phase 2: Shooting, Walls & Ricochet
**Goal**: Players can fire bullets that respect the map — blocked by walls, bouncing a limited number of times — with both players seeing identical shots
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: COMBAT-02, COMBAT-05, COMBAT-06
**Success Criteria** (what must be TRUE):
  1. Pressing fire spawns a bullet that both players see travel along the same path at the same time
  2. Tanks cannot drive through walls or obstacles — movement stops at the wall on both screens
  3. Bullets bounce off walls and keep traveling, then disappear once the bounce limit is reached
  4. When a bullet reaches a tank, the server decides the hit — both players see the same hit-or-miss outcome, and a client cannot claim a hit it didn't earn
**Plans**: TBD

*Notes: Ricochet is grouped with wall collision deliberately — bullet↔wall resolution is written once with a bounce counter instead of being built as "stop" and rewritten as "bounce" later. Plan should enumerate collision edge cases (fast bullet skipping past a thin wall or tank, wall corners) rather than assuming naive AABB is enough.*

### Phase 3: Damage, Win Condition & Match Lifecycle
**Goal**: A match plays through to a definite, agreed finish — hits cost HP, zero HP declares a winner, and a disconnect never leaves a player stuck
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: COMBAT-03, COMBAT-04, LIFE-01, LIFE-02
**Success Criteria** (what must be TRUE):
  1. Getting hit visibly reduces the hit tank's HP, and both players see the same HP values for both tanks
  2. When a tank's HP reaches 0 the match stops immediately and both players are shown the same winner
  3. If one player closes their browser mid-match, the remaining player sees the match end within a few seconds with themselves declared the winner
  4. Every match ending — win, loss, or forfeit — lands on a clear result screen rather than a frozen game view
**Plans**: TBD
**UI hint**: yes

### Phase 4: Accounts, Matchmaking & Leaderboard
**Goal**: Real named players register, log in, get paired with an opponent automatically, and watch their win/loss record accumulate across matches
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: AUTH-01, AUTH-02, AUTH-03, MATCH-01, MATCH-02, LEADER-01
**Success Criteria** (what must be TRUE):
  1. A new user can register with a username and password, log in, and refresh the page without being logged out
  2. Two logged-in users who ask for a match are paired into the same match, each seeing a waiting status until an opponent is found
  3. Each player's UI shows the opponent's real username instead of a placeholder
  4. When a match finishes, both players' win/loss records update on a leaderboard that stays correct after the server restarts
**Plans**: TBD
**UI hint**: yes

*Notes: Standard, well-documented plumbing (jBCrypt + JWT, FIFO queue, H2 + JDBI) placed after the real-time core works. Planning should settle exactly how the token is validated at the WebSocket handshake (query param vs. subprotocol vs. first-message auth) and whether the server must support multiple simultaneous matches.*

### Phase 5: Practice Bot & In-Match Chat
**Goal**: The game is demo-complete without a second human — playable against an AI tank — and players can talk to each other during a live match
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: BOT-01, CHAT-01
**Success Criteria** (what must be TRUE):
  1. A player alone can start a practice match against an AI tank that moves, aims, and shoots well enough to be a real opponent
  2. The practice match runs the same rules as a real match — HP, walls, ricochet, and a result screen
  3. During a live match, a message typed by one player appears in the opponent's chat labeled with the sender's username
  4. Typing in chat never moves or fires the tank — game input and chat input do not collide
**Plans**: TBD
**UI hint**: yes

*Notes: Chat is routed through the connection layer, not the tick loop, so it cannot interfere with game-critical timing. Bot AI heuristics (aiming, ricochet-aware shots, movement) likely need research during planning.*

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Networked Movement Skeleton | 0/TBD | Not started | - |
| 2. Shooting, Walls & Ricochet | 0/TBD | Not started | - |
| 3. Damage, Win Condition & Match Lifecycle | 0/TBD | Not started | - |
| 4. Accounts, Matchmaking & Leaderboard | 0/TBD | Not started | - |
| 5. Practice Bot & In-Match Chat | 0/TBD | Not started | - |

## Requirement Coverage

| Requirement | Phase |
|-------------|-------|
| COMBAT-01 | Phase 1 |
| COMBAT-02 | Phase 2 |
| COMBAT-05 | Phase 2 |
| COMBAT-06 | Phase 2 |
| COMBAT-03 | Phase 3 |
| COMBAT-04 | Phase 3 |
| LIFE-01 | Phase 3 |
| LIFE-02 | Phase 3 |
| AUTH-01 | Phase 4 |
| AUTH-02 | Phase 4 |
| AUTH-03 | Phase 4 |
| MATCH-01 | Phase 4 |
| MATCH-02 | Phase 4 |
| LEADER-01 | Phase 4 |
| BOT-01 | Phase 5 |
| CHAT-01 | Phase 5 |

**Coverage:** 16/16 v1 requirements mapped, no orphans, no duplicates.

---
*Roadmap created: 2026-08-13*
