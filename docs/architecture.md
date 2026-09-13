# Architecture

## Tổng quan

Server-authoritative real-time PvE game. Client chỉ gửi input; server xử lý toàn bộ logic, broadcast state 30Hz.

```
┌──────────────────┐                            ┌──────────────────────────────┐
│   Browser        │                            │     Spring Boot (8081)        │
│  ┌────────────┐  │   WebSocket /game          │ ╔══════════════════════════╗  │
│  │  React     │◄─┼───────────────────────────►│ ║   WebSocket Handler     ║  │
│  │  Canvas    │  │   JOIN_GAME/ MOVE/ SHOOT   │ ║   GameWebSocketHandler  ║  │
│  │  (iframe)  │  │   ◄── STATE / ACK / ERROR  ║   ↓                       ║  │
│  └────────────┘  │                            │ ║   GameSessionService     ║  │
│                  │                            │ ║   ↓ queueInput           ║  │
│  Thymeleaf       │   REST                     │ ║   GameLoop (30Hz)        ║  │
│  pages           │◄─┼────────────────────────►│ ║   ├ processInputs        ║  │
│  / /campaign     │   GET / POST /api/*        │ ║   ├ updateEntities       ║  │
│  /play           │                            │ ║   ├ checkCollisions      ║  │
│  /leaderboard    │                            │ ║   ├ broadcastState       ║  │
│                  │                            │ ║   └ cleanupDeadTanks     ║  │
└──────────────────┘                            │ ╚══════════════════════════╝  │
                                                 │              ↓               │
                                                 │   StatePublisherPort         │
                                                 │   (WebSocketStatePublisher)  │
                                                 │                              │
                                                 │   MatchEndObserver (@Async)  │
                                                 │   └─> LeaderboardService     │
                                                 │   └─> PlayerProgressService  │
                                                 │              ↓               │
                                                 │   JPA Repositories           │
                                                 │              ↓               │
                                                 │   PostgreSQL 16              │
                                                 └──────────────────────────────┘
                                                          ↑
                                                  Docker compose
                                                  infra/docker-compose.yml
                                                  Volume: tankzors_pg_data
```

## Package layout (server-spring)

```text
com.tankzors/
├── TankzorsApplication.java       # @SpringBootApplication
├── ServerConfig.java              # PORT, TICK_RATE, ...
│
├── config/
│   ├── EngineConfig.java          # @Bean GameState, GameLoop, parser, @EnableAsync
│   ├── EngineLifecycle.java       # @PostConstruct start / @PreDestroy stop
│   └── GsonConfig.java            # Gson bean
│
├── engine/                        # clean: chỉ phụ thuộc interface port
│   ├── GameLoop.java              # core 30Hz loop
│   ├── GameState.java             # tanks + bullets + map + status
│   ├── CollisionManager.java      # static AABB helpers
│   ├── BulletManager.java
│   ├── StatePublisherPort.java    # interface port -> WebSocket adapter
│   └── MatchEndObserver.java      # interface port -> async persistence
│
├── entity/                        # domain entities, không có Spring/JPA
│   ├── Tank.java (abstract)
│   ├── PlayerTank.java
│   ├── AITank.java
│   ├── Bullet.java
│   ├── Direction.java
│   ├── Position.java
│   ├── Rectangle.java
│   ├── Wall.java
│   └── Item.java
│
├── ai/                            # AI controllers và behaviors
│   ├── AIController.java
│   ├── ChaseBehavior.java
│   ├── PatrolBehavior.java
│   ├── ShootBehavior.java
│   ├── IdleBehavior.java
│   ├── AINavigator.java
│   ├── GridPathfinder.java
│   ├── LineOfSight.java
│   └── DifficultyLevel.java       # EASY/MEDIUM/HARD tuning
│
├── map/
│   ├── GameMap.java               # IMap implementation
│   ├── MapLoader.java             # load campaign_*.json từ classpath
│   └── TileType.java              # enum mapping
│
├── interfaces/                    # SOLID contracts
│   ├── ITank.java
│   ├── IBullet.java
│   ├── IMap.java
│   ├── IWeapon.java
│   ├── ICollidable.java
│   ├── IAIBehavior.java
│   ├── IGameObserver.java
│   └── IUpdatable.java
│
├── application/                   # use case services
│   ├── GameSessionService.java    # queue input tới GameLoop
│   ├── LeaderboardService.java    # upsert best score
│   ├── PlayerProgressService.java # unlock màn kế tiếp
│   ├── MatchEndService.java       # @Async hook VICTORY/DEFEAT
│   └── ScoreCalculator.java       # công thức HP*10 + AI*200 + max(0,1000-t*10)
│
├── leaderboard/                   # JPA persistence
│   ├── LeaderboardEntry.java      # @Entity
│   └── LeaderboardRepository.java # Spring Data JPA
│
├── progress/                      # JPA persistence (Phase 5+)
│   ├── PlayerProgress.java        # @Entity composite PK
│   └── PlayerProgressRepository.java
│
├── network/                       # WebSocket adapter layer
│   ├── WebSocketConfig.java       # đăng ký /game
│   ├── GameWebSocketHandler.java  # route packet
│   ├── WebSocketStatePublisher.java # impl StatePublisherPort
│   ├── SessionRegistry.java       # WebSocketSession map
│   ├── InputPacketParser.java     # flat hoá nested payload
│   └── protocol/                  # DTO legacy (InputMessage, StateMessage, ...)
│
├── web/                           # HTTP controllers
│   ├── HealthController.java      # GET /api/health
│   ├── LeaderboardController.java # REST leaderboard
│   ├── ProgressController.java    # REST progression
│   └── PageController.java        # Thymeleaf pages
│
└── resources/
    ├── application.yml            # main profile (Postgres)
    ├── application-test.yml       # test profile (H2 in-memory)
    ├── db/migration/
    │   ├── V1__init.sql           # leaderboard + match_result
    │   └── V2__player_progress.sql
    ├── maps/                      # campaign_1/2/3.json
    ├── templates/                 # Thymeleaf: layout, home, campaign, play, leaderboard
    └── static/play/               # React build (vite dist)
```

## SOLID highlights

### Single Responsibility (SRP)

| Class | Trách nhiệm duy nhất |
|---|---|
| `GameLoop` | tick simulation 30Hz, không biết transport |
| `GameWebSocketHandler` | parse packet → enqueue input |
| `WebSocketStatePublisher` | serialize + broadcast JSON, không biết game rule |
| `MapLoader` | load JSON từ classpath, không biết collision |
| `ScoreCalculator` | công thức điểm, không biết JPA |

### Open/Closed (OCP)

Thêm vũ khí/behavior mới: implement `IWeapon` hoặc `IAIBehavior`, không phải sửa engine.

### Liskov (LSP)

`PlayerTank`, `AITank` đều substitute được cho `Tank`/`ITank`. `DifficultyLevel.EASY/MEDIUM/HARD` cùng interface, chỉ đổi tuning.

### Interface Segregation (ISP)

8 interface nhỏ (`ITank`, `IBullet`, `IMap`, `ICollidable`, `IWeapon`, `IAIBehavior`, `IGameObserver`, `IUpdatable`) — class implement theo nhu cầu.

### Dependency Inversion (DIP)

`GameLoop` chỉ biết interface port:
- `StatePublisherPort` (engine) ← impl `WebSocketStatePublisher` (network)
- `MatchEndObserver` (engine) ← impl `MatchEndService` (application, async)

Cụ thể: **engine package không import gì từ network/application/spring**. Verify:

```bash
grep -r "import org.springframework" server-spring/src/main/java/com/tankzors/engine/
# (no output) - engine clean
```

## GameLoop dataflow

Mỗi tick (33ms):

```
tick(deltaTime)
 ├─ processInputs()
 │   ├─ poll ConcurrentLinkedQueue<PlayerInput>
 │   └─ applyInput(sessionId, InputMessage)
 │       ├─ JOIN_GAME  -> handleJoinGame (load map, spawn, setStatus PLAYING)
 │       ├─ MOVE_INPUT -> player.setMoving(direction, moving)
 │       ├─ STOP       -> player.stopMoving / setMoving(direction, false)
 │       ├─ SHOOT_INPUT-> tank.shoot() -> addBullet
 │       └─ LEAVE_GAME -> removeTank
 │
 ├─ updateEntities(dt)   nếu status == PLAYING
 │   ├─ player.updateCooldown(dt)
 │   ├─ player.updateMovement(dt, map)  -> resolveCollision sliding
 │   ├─ ai.update(state, dt)            -> AIController + behaviors
 │   ├─ bullet.update(dt)               -> position += velocity*dt
 │   └─ removeInactiveBullets
 │
 ├─ checkCollisions()
 │   ├─ bullet vs wall  -> deactivate
 │   ├─ bullet vs tank  -> takeDamage (skip if ownerId==tankId)
 │   │                    if !alive: gameState.addScore(100), aiKilledThisMatch++
 │   └─ loop break sau khi va chạm đầu
 │
 ├─ broadcastState()  nếu status != WAITING
 │   ├─ gameState.toStateMessage() -> StateMessage (tanks/bullets/map)
 │   └─ statePublisher.broadcast(stateMsg)
 │
 ├─ cleanupDeadTanks()  nếu status == PLAYING
 │   ├─ remove tanks có alive == false
 │   ├─ if !hasPlayer  -> setStatus(DEFEAT), broadcastState, fireMatchEnd(DEFEAT)
 │   └─ if !hasAI      -> setStatus(VICTORY), broadcastState, fireMatchEnd(VICTORY)
 │
 └─ incrementTick()

fireMatchEnd(status, hpRemain):
  matchEndObserver.onMatchEnd(sessionId, levelId, status, hpRemain, aiKills, elapsedSeconds)
    └─> MatchEndService @Async:
          if VICTORY:
            score = ScoreCalculator.calculate(hpRemain, aiKills, elapsedSec)
            leaderboardService.submitScore(playerName, levelId, score, ...)
            progressService.markVictory(sessionId, levelId)  // unlock màn kế tiếp
```

## Threading model

| Thread | Vai trò |
|---|---|
| `main` | Spring Boot startup + Tomcat |
| `http-nio-8081-exec-N` | Request HTTP/REST, Thymeleaf render |
| `WebSocket-Worker-N` | WebSocket inbound (Json parse + queue input) |
| `GameLoop-Thread` | **single-threaded** chạy `tick()` 30Hz, là nguồn thật của state |
| `task-N` | `@Async` Spring pool cho `MatchEndService.onMatchEnd` (DB call) |
| `GameLoop-Thread` | `WebSocketStatePublisher.broadcast` cũng running ở đó |

Lý do single-thread: tránh lock nguy cơ race khi update entity. Input từ network thread -> queue, loop poll tuần tự.

## Database schema

```sql
-- V1
CREATE TABLE leaderboard (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  player_name VARCHAR(50) NOT NULL,
  map_id VARCHAR(30) NOT NULL,
  score INTEGER NOT NULL,
  time_seconds INTEGER NOT NULL DEFAULT 0,
  hp_remain INTEGER DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT leaderboard_unique UNIQUE (player_name, map_id)
);

CREATE TABLE match_result (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  session_id VARCHAR(80),
  player_name VARCHAR(50),
  map_id VARCHAR(30),
  mode VARCHAR(20),
  status VARCHAR(20),
  score INTEGER DEFAULT 0,
  time_seconds INTEGER DEFAULT 0,
  hp_remain INTEGER DEFAULT 0,
  started_at TIMESTAMP,
  ended_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2
CREATE TABLE player_progress (
  session_id VARCHAR(80) NOT NULL,
  unlocked_level VARCHAR(30) NOT NULL,
  PRIMARY KEY (session_id, unlocked_level)
);
```

`Flyway` quản lý, `application.yml` có `spring.flyway.enabled=true`. Mỗi lần server boot, check migration mới và apply.
