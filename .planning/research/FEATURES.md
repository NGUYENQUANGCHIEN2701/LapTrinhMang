# Feature Research

**Domain:** Real-time 1v1 multiplayer 2D tank/arena battle game (course project, client-server)
**Researched:** 2026-08-13
**Confidence:** HIGH (genre conventions are well-established and stable; course-scope judgment calibrated against known project constraints in PROJECT.md)

## Feature Landscape

### Table Stakes (Users Expect These)

For a gradeable course demo, "users" = the grader watching two browser tabs play a live match. Missing any of these makes the demo feel broken or the deliverable feel incomplete, regardless of polish elsewhere.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Account register/login | Any "leaderboard" or persistent identity requires knowing who played. Baseline for any online game. | MEDIUM | Password hashing (bcrypt), session or JWT, minimal user table. Already decided (PROJECT.md). |
| 1v1 match join (queue or room code) | Two players need a deterministic way to end up in the same match instance. | LOW–MEDIUM | Simplest correct version: a "waiting room" queue that pairs the first two waiting sockets, or a shareable room code. Skill-based matchmaking is explicitly out of scope. |
| Real-time tank movement + shooting sync | This is the core value proposition per PROJECT.md ("client and server actually communicating live game state"). Without it the project isn't a network programming demo, it's a local game. | HIGH | Server-authoritative loop broadcasting position/state at a fixed tick rate (e.g. 20–30Hz) is sufficient — see Anti-Features for what NOT to build here. |
| Tank HP + win/lose condition | A "battle" without a resolvable outcome isn't a game, it's a toy. Already decided. | LOW | Straightforward once hit-detection and damage messages flow through the same server-authoritative loop. |
| Map with walls/obstacles (block movement + bullets) | Distinguishes this from a featureless open-field shooter; also the thing that makes 2D tank games (Battle City, Wii Tanks) recognizable as the genre. Already decided. | MEDIUM | Requires 2D AABB or grid collision for both tank movement and bullet travel — two separate collision checks against the same wall data. |
| In-match text chat | Already decided; also doubles as a cheap, low-risk way to demonstrate a second real-time message type over the same connection (useful to show the grader you built a general message protocol, not a single-purpose one). | LOW | Reuses the already-open match WebSocket; no separate infra needed. |
| Leaderboard (win/loss record) | Already decided; gives the account system a purpose beyond login and gives the grader a persistent artifact to inspect after matches. | LOW | Simple aggregate query (wins, losses, maybe win rate) over stored match results, sorted. |
| Disconnect / forfeit handling | Real network programming inherently has to handle a peer vanishing mid-session. A demo that hangs forever (or crashes the server) when one tab closes reads as an unfinished project, and this is precisely the kind of "network" edge case a Network Programming course grades on. | MEDIUM | Detect socket close/timeout server-side, end the match with the remaining player as winner (or void it), notify the other client. |
| Post-match result screen | Closes the loop from "match in progress" to "leaderboard updated." Without it, players (and the grader) can't tell the match actually ended and was recorded. | LOW | "You Win / You Lose" + basic stats, triggered by the same server message that ends the match. |
| Basic collision detection (tank↔wall, bullet↔wall, bullet↔tank) | Prerequisite for HP loss, map obstacles, and match resolution all being meaningful. | MEDIUM | Not a standalone feature so much as shared plumbing — call it out explicitly because it's easy to underestimate. |

### Differentiators (Competitive Advantage)

Not required for a passing/complete demo, but each one visibly raises the "this feels like a real game, not a class assignment" bar. Pick 1–2 once table stakes are solid and stable — don't spread thin across all of them.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Bullet ricochet off walls | The single most genre-defining mechanic in top-down tank games (Wii Play: Tanks! built its entire skill ceiling around bullets bouncing off blocks, forcing "predictive aiming and angular judgement"). Cheap to explain, high perceived polish. | MEDIUM | Reflect bullet velocity vector across the wall's collision normal on impact; cap bounce count (e.g. 2–3) to avoid runaway state. |
| Power-ups on the map (speed boost, shield, extra damage, heal) | Classic Battle City/arcade-tank staple; adds variety to an otherwise static 1v1 duel with minimal new systems (reuses map + collision + HP plumbing already built). | MEDIUM | Spawn on a timer at fixed points, apply a timed stat modifier on pickup collision. |
| Practice bot / AI opponent | Lets you (the solo dev) test and demo the core loop without needing two humans present — genuinely useful during development, and doubles as a fallback demo mode if a live second player isn't available at grading time. | MEDIUM | Simple heuristic AI (move toward player, shoot on line-of-sight) reusing the same server-side match loop a real second player would drive. |
| Multiple tank types / stat presets | Adds replay variety and a "build" decision for players with very little new code — just different HP/speed/fire-rate/damage numbers selected at match start. | LOW–MEDIUM | Do this before "tank customization" — it's the 20% effort version of the same idea. |
| Multiple selectable maps | Shows the wall/obstacle system is data-driven, not hardcoded — a nice signal of solid architecture in a course context. | LOW–MEDIUM | Only worth it once one map's collision logic is fully correct and stable. |
| Match history / per-match stats (shots fired, accuracy, damage dealt) | Extends the leaderboard from "who won" to "how they played" — cheap to log since the server already sees every shot/hit event. | LOW–MEDIUM | Append-only log of events already flowing through the server loop; just needs storage + a display page. |
| Sound effects and simple visual polish (explosions, muzzle flash, screen shake) | Cheapest possible perceived-quality boost; doesn't touch networking code at all. | LOW–MEDIUM | Purely client-side (React); safe to do late without risking the networking core. |
| Reconnect into an in-progress match | Genuinely nice UX (accidental refresh doesn't end your game) but is one of the hardest problems in real-time netcode — full state resync on rejoin. | HIGH | Treat as a stretch goal only after everything else above is done; do not let this block core delivery. |

### Anti-Features (Commonly Requested, Often Problematic)

These map directly onto known genre conventions from commercial tank games (Tanki Online, Tanks a Lot, War of Tanks) — exactly the features that look "expected" if you benchmark against the App Store, but that blow the scope of a solo, several-week, gradeable client-server assignment.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|------------------|-------------|
| >2 players / team battles (3v3, battle royale, 10v10) | Nearly every commercial tank game (Tanks a Lot, War of Tanks) ships team modes as the default experience. | State-sync complexity scales non-linearly with player count — more concurrent input streams, more collision pairs, harder to reason about and debug in the time available. Already excluded in PROJECT.md. | Ship 1v1 well; mention team modes as documented future work if asked. |
| Skill-based matchmaking / ELO rating | Commercial games use it to keep matches competitive at scale. | Needs a rating algorithm, a match history to feed it, and enough concurrent users to make matching meaningful — none of which exist in a 2-player course demo. Already excluded. | Win/loss counts on the leaderboard (already scoped) fully cover "who's good" for this audience. |
| Spectator mode / match replay | Looks impressive in a demo video. | Requires either a separate read-only broadcast channel or full state-log recording and playback — a second delivery pipeline on top of the live one. Already excluded. | If a demo needs an audience, just share a screen; don't build it into the product. |
| Native mobile app | Commercial tank games are primarily mobile (iOS/Android app store listings dominate this genre). | Different rendering/input stack, app store packaging, touch controls — none of it validates the networking coursework. Already excluded. | React web client, responsive layout at most. |
| Tank progression / upgrade trees + persistent currency, in-game shop | This is the core retention loop of every commercial tank game (Tanki Online, Tanks a Lot: parts, upgrades, currency). | Turns the project into an economy-design problem (balancing, grinding, persistence of dozens of unlockable stats) that has nothing to do with the networking learning objective and can consume the entire timeline on its own. | Multiple tank *types* picked per-match (a differentiator above) gets 80% of the "variety" feeling for ~5% of the effort. |
| Voice chat | Feels like the "real" multiplayer experience. | Requires WebRTC/audio streaming infrastructure completely separate from the game's WebSocket protocol — a second, much harder networking subsystem for marginal value over text. | In-match text chat (already scoped) is sufficient and reuses existing infrastructure. |
| Full client-side prediction + server reconciliation + lag compensation (rollback netcode) | This is the "correct" AAA answer to real-time multiplayer, and it's the kind of thing network-programming coursework material often name-drops. | It is genuinely one of the hardest problems in multiplayer engineering (input buffering, rewind-and-replay, interpolation) and is overkill for a 1v1 game played over short distances/low latency in a grading context — the complexity-to-payoff ratio is very poor for a solo dev on a deadline. | A simple server-authoritative loop (server simulates at fixed tick, broadcasts state, client just renders what it's told) is standard, well-understood, and entirely sufficient at this scale. If input lag is visibly bad, add basic client-side prediction for movement only — not full reconciliation. |
| Anti-cheat systems | "Real" multiplayer games need them. | There's no economy, ranking, or real stakes to protect in a course project; time spent here is time not spent finishing table-stakes features. | Server-authoritative game state (already the plan) is itself the anti-cheat baseline — trust nothing the client says about position/HP, only accept inputs. That's sufficient. |
| Custom map editor | Feels like a natural extension of "map with obstacles." | A full editor UI is a second application bolted onto the game. | Author 1–3 hardcoded/JSON map layouts yourself; that satisfies "map with walls" without a content-authoring tool. |
| Global lobby chat, friends list, DMs | Feels like it "completes" the social features started by in-match chat. | Expands chat from a per-match, already-open-connection feature into a persistent, always-on presence/social system — new infrastructure with no bearing on the core 1v1 battle loop. | In-match chat only (already scoped). |
| Achievements / badges | Common retention feature in commercial games. | Pure scope addition with no relationship to the networking objective; tends to multiply "just one more small feature" requests. | Leaderboard (already scoped) is the single progression signal needed. |

## Feature Dependencies

```
Account (register/login)
    └──requires──> nothing (foundational)
    └──enables──> 1v1 match join (need an identity to match)
                       └──enables──> Real-time match (move/shoot sync)
                                          ├──requires──> Map (walls/obstacles + collision)
                                          ├──requires──> Tank HP + win/lose logic
                                          ├──enables──> In-match chat (piggybacks the open match connection)
                                          ├──enables──> Disconnect/forfeit handling
                                          └──produces──> Match result
                                                             └──enables──> Post-match result screen
                                                             └──enables──> Leaderboard update (requires Account identity)

Map + collision system ──enables──> Bullet ricochet (differentiator)
Map + collision system ──enables──> Power-ups (differentiator, also requires HP/stat system)
Tank HP/stat system ──enables──> Multiple tank types (differentiator)
Real-time match loop ──enables──> Practice bot (differentiator; bot plugs into match loop as a fake second client)
Match result logging ──enables──> Match history / per-match stats (differentiator)
Real-time match loop ──enables──> Reconnect-to-match (differentiator; requires the loop to already be rock solid)
```

### Dependency Notes

- **1v1 match join requires Account:** you can't credit a win/loss to an anonymous socket if the leaderboard is going to mean anything — build login before matchmaking, not in parallel.
- **Real-time match requires Map + HP/win-lose as day-one dependencies, not add-ons:** a match loop that only handles movement and then bolts on collision and HP later tends to require reworking the state/broadcast format. Design the tick payload (position, HP, wall layout) together from the start.
- **In-match chat and Disconnect handling both piggyback the same live match connection:** build them right after the core movement/shoot sync is stable — they're cheap additions to plumbing that already exists, not separate subsystems.
- **Leaderboard requires a completed match result, which requires Account:** this is the natural last link in the chain — don't build leaderboard UI before match-end events exist to feed it.
- **Bullet ricochet and Power-ups both require the Map/collision system to be correct first:** ricochet math depends on knowing wall collision normals; power-up pickup depends on the same collision detection used for bullet↔tank. Don't attempt these while wall collision is still buggy.
- **Practice bot conflicts with nothing and can be built early as a dev tool:** since it just drives inputs into the existing match loop as a second "client," it's a good candidate to build right after the match loop works, purely to enable solo testing — even if you decide not to ship it as a player-facing feature.
- **Reconnect-to-match should be attempted last, if at all:** it depends on every other system (auth session, match state, map state, HP) all being stable enough to serialize and resume — attempting it before the core loop is solid is a common way to lose a week debugging state desync instead of finishing table stakes.

## MVP Definition

### Launch With (v1) — the gradeable deliverable

- [ ] Account register/login — no leaderboard or match identity without it
- [ ] 1v1 match join (simple queue or room code) — minimum viable pairing mechanism
- [ ] Real-time tank movement + shooting sync — the core networking demo
- [ ] Tank HP + win/lose condition — makes it a game, not a toy
- [ ] Map with walls/obstacles (movement + bullet blocking) — genre-defining, already decided
- [ ] In-match text chat — already decided, cheap given the open connection
- [ ] Leaderboard (win/loss record) — already decided, gives accounts a purpose
- [ ] Disconnect/forfeit handling — required so the demo can't hang or crash on a closed tab
- [ ] Post-match result screen — closes the loop visibly for the grader

### Add After Validation (v1.x)

Only after the above is fully working end-to-end and stable across repeated matches:

- [ ] Bullet ricochet off walls — trigger: core movement/shoot/collision loop is bug-free
- [ ] Power-ups on the map — trigger: same, plus HP/stat system has spare hooks for timed modifiers
- [ ] Practice bot / AI opponent — trigger: useful as soon as the match loop exists, even before matchmaking is polished (build early as a dev/testing aid; decide later whether to expose it as a real mode)
- [ ] Sound/visual polish — trigger: any time, since it's purely client-side and can't destabilize networking

### Future Consideration (v2+, likely never for this course)

- [ ] Multiple tank types — defer until 1v1 core is proven fun/stable
- [ ] Multiple maps — defer until one map's collision is fully correct
- [ ] Match history / detailed stats — defer until basic leaderboard is proven
- [ ] Reconnect-to-match — defer indefinitely; only attempt if significant time remains after everything above

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Account login | HIGH | MEDIUM | P1 |
| 1v1 match join | HIGH | MEDIUM | P1 |
| Real-time move/shoot sync | HIGH | HIGH | P1 |
| Tank HP + win/lose | HIGH | LOW | P1 |
| Map walls/obstacles + collision | HIGH | MEDIUM | P1 |
| In-match chat | MEDIUM | LOW | P1 |
| Leaderboard | MEDIUM | LOW | P1 |
| Disconnect/forfeit handling | HIGH | MEDIUM | P1 |
| Post-match result screen | MEDIUM | LOW | P1 |
| Bullet ricochet | MEDIUM | MEDIUM | P2 |
| Power-ups | MEDIUM | MEDIUM | P2 |
| Practice bot | MEDIUM | MEDIUM | P2 |
| Sound/visual polish | LOW-MEDIUM | LOW | P2 |
| Multiple tank types | LOW-MEDIUM | LOW-MEDIUM | P3 |
| Multiple maps | LOW | LOW-MEDIUM | P3 |
| Match history/stats | LOW-MEDIUM | LOW-MEDIUM | P3 |
| Reconnect-to-match | LOW | HIGH | P3 |
| Team modes (>2 players) | — | HIGH | Never (anti-feature) |
| ELO/rating system | — | MEDIUM-HIGH | Never (anti-feature) |
| Spectator/replay | — | HIGH | Never (anti-feature) |
| Voice chat | — | HIGH | Never (anti-feature) |
| Full rollback netcode | — | VERY HIGH | Never (anti-feature) |

**Priority key:**
- P1: Must have for launch (the graded deliverable)
- P2: Should have, add when possible without risking P1 stability
- P3: Nice to have, only if significant time remains

## Competitor Feature Analysis

Reference points are commercial tank games and the closest classic genre ancestor — used to calibrate what's "expected of the genre" vs "expected of this specific project."

| Feature | Battle City / Wii Play: Tanks! (classic 2D top-down) | Tanki Online / Tanks a Lot (commercial mobile/web) | Our Approach |
|---------|--------------------------------------------------------|------------------------------------------------------|--------------|
| Match size | Single player vs AI, or local multiplayer | 3v3, 8-player free-for-all, 10v10 | 1v1 only — matches PROJECT.md scope decision |
| Bullets vs walls | Bullets ricochet off blocks; core skill mechanic | Bullets typically absorbed by cover | Table stakes: bullets stop at walls. Ricochet as a differentiator if time allows. |
| Progression | None (arcade, session-based) | Deep upgrade trees, currency, unlockable parts | Explicitly out of scope — see Anti-Features |
| Matchmaking | N/A (local) | Skill-based queues | Simple queue/room join — see Anti-Features for why not skill-based |
| Chat | None | In-game team/global chat | In-match chat only, already scoped |
| Social/meta | None | Clans, friends, spectating | Explicitly out of scope — see Anti-Features |

## Sources

- [Wii Play/Tanks! — StrategyWiki](https://strategywiki.org/wiki/Wii_Play/Tanks!) — bullet ricochet as the genre-defining mechanic in classic top-down tank games
- [Battle City (1985) — MobyGames](https://www.mobygames.com/game/8277/battle-city/) — classic top-down tank game conventions (walls, power-ups)
- [Tanks a Lot: 3v3 Tank Battles — App Store](https://apps.apple.com/us/app/tanks-a-lot-3v3-tank-battles/id1344713773) — commercial team-mode tank game feature set (used as anti-pattern reference for team modes/progression)
- [Battle Tanks: Tank War Games — App Store](https://apps.apple.com/us/app/battle-tanks-tank-war-games/id1442191403) — commercial mobile tank game feature conventions
- [Solo Game Dev: Avoiding Scope Creep and Staying on Track — Wayline](https://www.wayline.io/blog/solo-game-dev-scope-creep-stay-on-track) — solo-dev scope discipline patterns applied to Anti-Features rationale
- [Essential Scope Rules for Successful Solo Game Dev — Wayline](https://www.wayline.io/blog/essential-scope-rules-solo-game-dev) — scope-constraint reasoning
- `/home/nqc/Documents/LapTrinhMang/.planning/PROJECT.md` — project-specific decided requirements and out-of-scope list (authoritative source for already-decided table stakes and anti-features)

---
*Feature research for: real-time 1v1 multiplayer tank battle game (course project)*
*Researched: 2026-08-13*
