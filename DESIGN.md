# Yars' Revenge Remake — Design Document

A faithful remake of the Atari 2600 classic, built in Java 21 + JavaFX 21. This document describes the
architecture, data flow, entity model, rendering pipeline, audio system, and extension points.

---

## Table of contents

1. [High-level overview](#1-high-level-overview)
2. [Package map](#2-package-map)
3. [Startup and scene management](#3-startup-and-scene-management)
4. [Game loop and timing](#4-game-loop-and-timing)
5. [Game state and phases](#5-game-state-and-phases)
6. [Entity model](#6-entity-model)
7. [Wave system](#7-wave-system)
8. [Shield system](#8-shield-system)
9. [Collision detection](#9-collision-detection)
10. [Rendering pipeline](#10-rendering-pipeline)
11. [Audio system](#11-audio-system)
12. [Input and autopilot](#12-input-and-autopilot)
13. [Game modes](#13-game-modes)
14. [Scoring](#14-scoring)
15. [Configuration and persistence](#15-configuration-and-persistence)
16. [Logging](#16-logging)
17. [Coordinate system](#17-coordinate-system)
18. [Extension points](#18-extension-points)

---

## 1. High-level overview

```
┌──────────────────────────────────────────────────────────────────────┐
│  YarsRevengeApp (JavaFX Application)                                 │
│  ┌────────────┐  ┌─────────────────────────────────────────────────┐│
│  │ SceneManager│  │ GameScreen                                      ││
│  │ (singleton) │  │  ┌──────────┐  ┌────────────┐  ┌────────────┐ ││
│  │             │  │  │GameState │  │GameLoop    │  │GameRenderer│ ││
│  │ switchTo()  │  │  │(model)   │◄─│(AnimTimer) │  │(canvas)    │ ││
│  │             │  │  └──────────┘  │  tick()    │  └────────────┘ ││
│  └────────────┘  │                 │  render()  │                  ││
│                  │  ┌────────────┐ └──────┬─────┘                  ││
│                  │  │GameCtrl    │        │calls                    ││
│                  │  │+ AutoPilot │◄───────┘                        ││
│                  │  └────────────┘                                  ││
│                  └─────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

The application holds a single `Stage` and `Scene` for its lifetime. `SceneManager` swaps the scene
root between screens (menu, game, high scores, etc.) without creating new windows.

---

## 2. Package map

| Package | Responsibility |
|---------|----------------|
| `app` | Entry point (`YarsRevengeApp`), `SceneManager`, `LogDirDefiner` |
| `config` | `GameConfig` — persisted settings (audio, FPS, resolution, game mode) |
| `model` | Pure game state: `GameState`, `GameConstants`, all entities, wave config |
| `model.entity` | All game entities plus `ShieldMode` enum |
| `model.wave` | `WaveConfig` record, `WaveFactory` |
| `controller` | `InputHandler` (keyboard events), `GameController`, `AutoPilot` |
| `engine` | `GameLoop` (fixed-timestep AnimationTimer), `CollisionDetector` |
| `renderer` | `GameRenderer` and sub-renderers, sprite drawers, `RenderContext` snapshot |
| `audio` | `PcmSynthesizer`, `AudioManager`, `SoundEffect` enum |
| `screen` | `Screen` interface and all screen implementations |
| `tools` | `IconGenerator` (offline utility, not used at runtime) |

---

## 3. Startup and scene management

`YarsRevengeApp.main()` parses `--debug` before calling `launch()`, promoting the
`com.yarsrevenge` logger to DEBUG via Logback's `LoggerContext` API. `start()` then:

1. Loads `GameConfig` (JSON file at `~/.config/yars-revenge-remake-einat2026/config.json`)
2. Creates a full-screen `Scene` over the primary screen bounds
3. Initialises `SceneManager` with the `Stage` and `Scene`
4. Pre-warms `AudioManager` (synthesises all WAV files, loads `AudioClip`s)
5. Wires Shift-M minimize handler (pauses game, exits fullscreen, iconifies)
6. Calls `SceneManager.showMainMenu()`

**`SceneManager`** is a singleton. Every screen transition calls `switchTo(Screen)` which
calls `hide()` on the outgoing screen and `show()` on the incoming one, then swaps the
scene root. This keeps the `Stage`/`Scene` alive for the whole session — no re-creation overhead.

---

## 4. Game loop and timing

`GameLoop` uses JavaFX `AnimationTimer` with a **fixed 60 Hz simulation timestep** and a
configurable render rate (6/15/30 FPS from `GameConfig`).

```
AnimationTimer.handle(nowNanos)
  │
  ├─ accumulate wall-clock delta (capped at 250ms to survive GC pauses)
  │
  ├─ while accumulator >= 1/60s:
  │    controller.processInput(1/60s, state)   ← input + autopilot
  │    tick(1/60s)                              ← simulation step
  │    accumulator -= 1/60s
  │
  ├─ drain audio events → AudioManager calls (safe on FX thread)
  │
  └─ if renderAccumulator >= renderInterval:
       renderer.render(gc, state)
```

Audio events are queued inside `tick()` via `state.queueAudio(AudioEvent)` and drained
once per frame *outside* the tick loop — this keeps `AudioClip.play()` off the simulation
hot path and avoids re-entrant FX thread calls.

**Frame gap logging**: any gap >200ms logs a WARN (e.g. FX thread blocked by GC or media init).

---

## 5. Game state and phases

`GameState` is the single source of truth for the running game. It holds all entities and
progresses through a `Phase` enum:

```
PLAYING ──► PAUSED ──► PLAYING
   │
   ├──► PLAYER_DYING (1.5s) ──► PLAYING (respawn, lives > 0)
   │                         └──► GAME_OVER (lives = 0)
   │
   └──► WAVE_TRANSITION (5s, explosion animation) ──► PLAYING (next wave)
```

State never calls `AudioManager` directly. It queues `AudioEvent` values that `GameLoop`
drains each frame. This keeps the model free of FX/audio dependencies.

**`initWave(n)`** resets all per-wave entities, builds the shield for the wave's mode, and
preserves the `Quotile` and `Torpedo` across waves (updating their speed/cooldown).

---

## 6. Entity model

All entities extend `GameEntity` (x, y, width, height, alive flag) with an
`update(double dt, GameState state)` method.

### Player (Yar)

- Moves via keyboard velocity (`setKeyboardVelocity`) or autopilot
- **Bounce**: nibbling a shield cell triggers a 50ms bounce-back at 400 px/s; `isBouncing()`
  blocks new velocity input during the bounce
- **Neutral Zone**: x=624–1040. While in the NZ, bullets and shots can't be fired, and
  the torpedo/shots pass harmlessly through
- **Wrap-around Y**: exits top → enters bottom and vice versa; X is clamped to screen

### Quotile (enemy)

Bounces vertically at wave speed. Cycles through three modes:

```
NORMAL ──(timer)──► MISSILE_WARNING ──(timer)──► MISSILE_FIRED
                                                      │
                          ◄──────── swirlFinished() ──┘
```

- **NORMAL**: fires `QuotileShot`s periodically; starts `MISSILE_WARNING` after 15s (0.1s at
  score ≥70k, effectively instant)
- **MISSILE_WARNING**: sounds alarm; transitions to `MISSILE_FIRED` after 4s (0.05s at score ≥70k)
- **MISSILE_FIRED**: spawns a `Swirl` aimed at the player's current position; waits for it to
  finish

### Swirl (missile)

Travels in a straight line toward the player's position at launch time. At score ≥150k,
enters **stall mode**: freezes for 0.55s every 1.2s to bait the player into moving into its path.
Stores `launchTargetY` (player Y when fired) for the autopilot dodge calculation.

### Torpedo (orb)

Indestructible homing projectile that chases the player every frame. Speed scales with wave.
The Neutral Zone offers no protection. Respawns to Quotile origin on wave start and player death.

### ZorlonCannon

Player's weapon. Travels rightward at 900 px/s from player's Y position at fire time.
- **Normal mode**: destroyed on shield contact
- **Rebound mode**: bounces off the shield and travels back leftward; if it hits the player,
  they die; if it hits the Quotile, it still kills
- **Ultimate mode**: see §13

### PlayerBullet

Short-range rightward projectile. Destroys shield cells but cannot harm Torpedo or Quotile.
One bullet in flight at a time; 250ms cooldown.

### QuotileShot

Leftward shot from the Quotile. Killed by Neutral Zone.

### Shield / ShieldCell

See §8.

---

## 7. Wave system

`WaveFactory.forWave(n)` linearly interpolates all parameters from wave 1 to wave 11+:

| Parameter | Wave 1 | Wave 11+ |
|-----------|--------|----------|
| Quotile speed | 70 px/s | 240 px/s |
| Torpedo speed | 0.75× | 2.25× (Novice: ×0.5) |
| Bullet cooldown | 2.5s | 0.7s |
| Shield rows | 20 | 10 |

Shield mode cycles: `ARCH_BARRICADE → CYCLING_FENCE → ROTATING_CIRCLE → RANDOM_SWARM → repeat`.

`CYCLING_FENCE` waves also apply an exponential speed multiplier (`1.1^(wave/2)`) to the torpedo
and Swirl, making even waves noticeably harder than their odd-wave neighbours.

---

## 8. Shield system

The shield always sits between the player zone and the Quotile. Its cells are 25×25 px squares.
The `Shield` class provides four geometries:

### ARCH_BARRICADE (odd waves, default)

Cells placed in a polar annular ring (radius 80–350 px) around the Quotile center, left half
only (dcol ≤ 1). Screen-edge clipped. Updates by recomputing each cell's position from its
stored polar coords + Quotile Y each tick.

### CYCLING_FENCE (wave 2, 6, 10, …)

A flat rectangular grid (`SHIELD_COLS × 1.3` wide, `shieldRows × 1.2` tall), centered
vertically and positioned left of the Quotile. Cells snake through a conveyor path:

- Row 0 (1-indexed odd): left → right
- Row 1 (1-indexed even): right → left
- Each cell advances one slot per 0.1s, wrapping to the start of the next row

The wall follows Quotile Y via a Y-offset applied each update.

### ROTATING_CIRCLE (wave 3, 7, 11, …)

Same polar geometry as ARCH_BARRICADE but a **full circle** (all angles, no dcol limit, no
screen-edge clipping). Cells orbit freely off-screen. Rotation at 40°/s.

### RANDOM_SWARM (wave 4, 8, 12, …)

3× the normal cell count, placed randomly in the left-half polar arc (angles 90°–270°, radii
80–350 px). Each cell has independent angular and radial drift velocities, bouncing off the
arc bounds. All cells follow Quotile Y each frame.

### Shield color thresholds

| Score | Cell color |
|-------|------------|
| < 70,000 | Red |
| 70,000–149,999 | Blue |
| 150,000–229,999 | Grey |
| ≥ 230,000 | Pink |

---

## 9. Collision detection

`CollisionDetector.detect(state)` runs every simulation tick. All checks use AABB intersection.
Order matters — each collision returns early after resolving.

| Collision | Effect |
|-----------|--------|
| Player nibbles shield cell | +61 EXP, bounce player back, charge cannon (Normal/Rebound) or +1 Tron (Ultimate) |
| Player touches Quotile | Charge cannon or +2 Trons (Ultimate) |
| Player hit by QuotileShot (outside NZ) | Player dies |
| Player hit by Torpedo (outside NZ, non-debug) | Player dies |
| PlayerBullet hits shield cell | +69 EXP, kill bullet and cell |
| Swirl hits player (no NZ protection) | Player dies |
| ZorlonCannon (bouncing) hits player | Player dies (Normal) or +4 Trons (Ultimate) |
| ZorlonCannon hits shield cell | Destroy cell; bounce cannon (Rebound) or kill cannon (others) |
| ZorlonCannon (non-bouncing) hits player | Player dies (self-kill) |
| ZorlonCannon hits Quotile | +1000 EXP, wave transition |
| ZorlonCannon hits Swirl (mid-air) | +6000 EXP, +1 life (capped 4), destroy Quotile, wave transition |

**`killPlayer`** is a no-op in debug mode.

`detectSwirlOnly` is called separately while a Swirl is in flight, after the main detect pass,
to catch the case where the Swirl hit the player in the same tick it was updated.

---

## 10. Rendering pipeline

All rendering runs on the JavaFX application thread after the simulation tick loop completes.
The canvas is a single `Canvas` sized to the physical screen. `GameRenderer` applies a scale
transform to map the 1920×1080 logical space to the draw region:

```
gc.translate(drawX, drawY)
gc.scale(drawW / 1920.0, drawH / 1080.0)
```

Letterboxing is supported: if the config resolution is smaller than the physical screen, the
game is centered with black bars.

**Render order** (back to front):

1. `BackgroundRenderer` — starfield, NZ boundary glow
2. `ShieldRenderer` — shield cells (color by score threshold)
3. `EntityRenderer` — Torpedo, Quotile/JetOctopus sprites, Swirl, ZorlonCannon, PlayerBullet,
   QuotileShot, Player/FlySprite
4. `HudRenderer` — EXP, lives, wave, mode, debug overlay
5. Phase overlays — pause menu, wave-clear explosion animation, player death flash

**`RenderContext`** is a value snapshot of `GameState` taken at the start of each render call.
This decouples the renderer from live mutable state so it never reads a partially-updated entity.

**First-frame GPU warmup**: on the very first render, all sprite types are drawn off-screen at
alpha=0 to force Prism to compile their shaders before the alarm sequence fires.

### Sprites

All sprites are procedurally drawn each frame using JavaFX `GraphicsContext`. There are no image
assets for entities — everything is vector/gradient-based:

- `FlySprite` — player ship with animated wing phase
- `QuotileSprite` / `JetOctopusSprite` — enemy (normal / alarm mode)
- `TorpedoSprite` — the homing orb

### Explosion animation

The wave-transition Quotile explosion is a 5-phase sequence (~5 seconds total) using
pre-baked random particle data (seeded RNG at class load time, stored in static float arrays):

1. White screen flash (0–0.2s)
2. Shockwave rings + particle burst + fireball (0.05–2.5s)
3. Starburst streaks (0.8–3.5s)
4. Color-cycling screen tint + second ring (1.5–4.5s)
5. Ember drift (2.5–5.0s)

---

## 11. Audio system

### PCM synthesis

All sounds are generated at startup by `PcmSynthesizer.generate(SoundEffect)` which returns
raw WAV bytes built from scratch (sine/noise/envelope math). There are no audio asset files
in the repository. The WAV bytes are written to temp files, loaded as `AudioClip`s, and
deleted on JVM exit.

**Why temp files**: JavaFX `AudioClip` does not support `data:` URIs. The temp-file approach
is the only way to feed synthesised audio into `AudioClip`.

### Loop management

Four persistent loops are managed by `AudioManager` using a `ScheduledExecutorService`
(single daemon thread `audio-loop-scheduler`):

| Loop | Sound | Volume |
|------|-------|--------|
| `bgHumFuture` | `BG_HUM` (4s period) | 0.75 |
| `cannonFlyFuture` | `CANNON_FLYING` (1.5s period) | 0.60 |
| `alarmFuture` | `ENEMY_ALARM` (240ms period) | 0.70 |
| `missileFuture` | `SWIRL` (1s period) | 0.75 |

Loops are scheduled with `scheduleAtFixedRate`. Exceptions inside the lambda are caught and
logged (an uncaught exception would silently cancel the future, stopping audio permanently).

### Audio event queue

Entities and game logic never call `AudioManager` directly. They call
`state.queueAudio(AudioEvent)`. `GameLoop` drains the queue once per frame after the tick
loop, on the FX thread. This ensures `AudioClip.play()` is always called from the FX thread
and that audio changes from multiple ticks in one frame are batched correctly.

### Sound effects

| SoundEffect | Type | Notes |
|-------------|------|-------|
| `BG_HUM` | Loop | Ambient hum, always playing during gameplay |
| `ENEMY_ALARM` | Loop | Plays during `MISSILE_WARNING` phase |
| `SWIRL` | Loop | Plays while Swirl is in flight |
| `CANNON_FLYING` | Loop | Plays while ZorlonCannon is in flight |
| `BULLET_SHOOT` | One-shot | Player bullet fire |
| `SHIELD_NIBBLE` | One-shot | Player eats a shield cell |
| `SHIELD_CELL_POP` | One-shot | Shield cell destroyed by cannon/bullet |
| `CANNON_LAUNCH` | One-shot | Cannon fired |
| `PLAYER_DEATH` | One-shot | Player dies |
| `QUOTILE_EXPLODE` | One-shot | Enemy explosion |
| `WAVE_START` | One-shot | Wave begin jingle |

---

## 12. Input and autopilot

### InputHandler

Attaches two `EventHandler`s to the `Scene` (via `addEventHandler`, not `setOnKeyPressed`, to
avoid overwriting the minimize filter). Maintains a `Set<KeyCode>` of currently held keys plus
one-shot "pending" booleans for action keys (ENTER, SPACE, ESCAPE, debug keys). Each pending
boolean is consumed exactly once via a `consume*()` method.

### Key bindings

| Key | Action |
|-----|--------|
| Arrow keys / WASD | Move |
| ENTER | Fire bullet |
| SPACE | Fire Zorlon Cannon |
| ESCAPE | Pause / resume |
| Shift-D | Toggle debug mode |
| Shift-A | Toggle autopilot |
| W (debug only) | Kill enemy (advance wave) |
| X (debug only) | +1000 EXP |
| Shift-M | Minimize (pauses game) |

### AutoPilot

Runs as a priority-ordered state machine every tick when enabled (Shift-A). Computes a
velocity target for the player, then calls `player.setKeyboardVelocity()`. Two fire flags
(`wantBullet`, `wantCannon`) are consumed by `GameController.handleAutoPilotFire()`.

**Priority order:**

1. **Dodge threats** — torpedo within 250px, Swirl warning/in-flight, own cannon within 200px Y.
   Evaluates 8 evenly-spaced compass directions from the player. Scores each:
   `score = distanceFromDanger − (shieldCellsWithin80px × 50)`. Always picks the best and
   moves — velocity is never zeroed while threatened. Resets the nibble commitment timer.

2. **Fire cannon** — cannon is charged (Normal/Rebound) or 5 Trons accumulated (Ultimate),
   AND nibble timer ≥ 3s. Aligns Y to Quotile from the player's current X position and fires.

3. **Nibble shield cells** — moves toward the nearest alive shield cell. Increments the
   nibble timer each tick. The 3s commitment prevents the autopilot from constantly
   interrupting nibbling to fire as soon as the cannon charges.

4. **Touch Quotile** — no shield cells remain and cannon is not charged. Moves to the
   Quotile body to trigger the charge collision.

5. **Patrol** — cannon is in flight and nothing else applies. Drifts to a resting position
   left of the Neutral Zone center.

**Swirl dodge details:**
- `MISSILE_WARNING`: navigate to screen center X (maximum lateral dodge room)
- Swirl in flight: move to the vertical extreme (top or bottom) opposite to `launchTargetY`

---

## 13. Game modes

Selected from the config screen before starting a game.

| Mode | Cannon behaviour | Special rule |
|------|-----------------|--------------|
| **Novice** | Standard | Torpedo speed ×0.5 |
| **Normal** | Fires once per charge, destroyed on shield hit | — |
| **Rebound** | Bounces off the shield back toward the player | Catching the rebound kills the player |
| **Ultimate** | Requires 5 Trons to fire; fired from left screen edge | Touching Quotile = +2 Trons; catching rebound = +4 Trons; no death from bouncing cannon |

**Trons** (Ultimate only): accumulated by nibbling shield cells (+1 each) or touching the
Quotile (+2). Reset to 0 on death and wave start.

---

## 14. Scoring

The in-game currency is called **EXP** throughout the UI (renamed from "score").

| Event | EXP |
|-------|-----|
| Nibble shield cell | +61 |
| Bullet destroys shield cell | +69 |
| Cannon destroys shield cell | +5 |
| Kill Quotile (cannon hit) | +1,000 |
| Kill Swirl mid-air with cannon | +6,000 |

**Life bonus**: killing the Swirl mid-air with the cannon grants +1 life (capped at 4).

**Score thresholds** affect gameplay:
- ≥70,000: Quotile fires missile almost instantly (MISSILE_WARNING 0.1s, phase 0.05s)
- ≥150,000: Swirl stalls mid-flight (freeze-bait tactic)

High scores are persisted to `~/.yars-revenge-scores` (one integer per line, max 10 entries).

---

## 15. Configuration and persistence

`GameConfig` stores settings as a hand-parsed JSON file at
`~/.config/yars-revenge-remake-einat2026/config.json`. No external JSON library is used.

| Setting | Values | Default |
|---------|--------|---------|
| `audioEnabled` | true/false | true |
| `fpsLimit` | 6, 15, 30 | 30 |
| `resolution` | FIT, 1920x1080, 1024x576, 3840x2160 | FIT |
| `gameMode` | NOVICE, NORMAL, REBOUND, ULTIMATE | NORMAL |

`FIT` fills the entire physical screen. Other resolutions are letterboxed/centered.

---

## 16. Logging

SLF4J API backed by Logback. Configured in `src/main/resources/logback.xml`.

**Destinations:**
- Console: `HH:mm:ss.SSS` pattern
- Rolling file: `yyyy-MM-dd HH:mm:ss.SSS` pattern, daily rollover, 7-day retention
- Log directory resolved at runtime by `LogDirDefiner`:
  - macOS: `~/Library/Logs/yars-revenge/`
  - Other: `~/.local/share/yars-revenge/logs/`

**Log levels:**
- `INFO` by default (app start, AudioManager init)
- `DEBUG` for audio lifecycle events (loop start/stop), entity mode transitions
- `WARN` for performance anomalies (frame gaps >200ms, slow frames >50ms, slow audio drain >5ms)
- `ERROR` for failures (clip generation, font load, config save)

**`--debug` flag**: pass as CLI arg to promote `com.yarsrevenge` to DEBUG before JavaFX
launches, capturing all audio and game-logic transitions.

To update to latest dependency versions: `mvn versions:use-latest-releases`

---

## 17. Coordinate system

- **Logical space**: 1920 × 1080 px. All game logic uses these coordinates.
- **Physical space**: full primary screen resolution (whatever the OS reports).
- `GameRenderer` applies `gc.scale(physW/1920, physH/1080)` after translating to the draw
  region, mapping logical → physical transparently.
- X increases rightward; Y increases downward (standard screen convention).
- Player is constrained to x ∈ [0, 1920−64].
- Player wraps vertically (exits top → enters bottom).
- Neutral Zone: x = 624–1040.
- Quotile: fixed at x ≈ 1824.
- Shield zone: starts around x = 1350.

---

## 18. Extension points

### Adding a new sound

1. Add entry to `SoundEffect` enum
2. Add a `case` in `PcmSynthesizer.generate()` returning a WAV `byte[]`
3. If it loops: register its duration in `AudioManager.clipDurationMs` and add
   start/stop methods matching the existing pattern
4. Add a corresponding `AudioEvent` value in `GameState.AudioEvent` and a `case` in
   `GameLoop`'s audio drain switch

### Adding a new entity

1. Create a class extending `GameEntity` with `update(double dt, GameState state)`
2. Add a field + getter to `GameState`; initialise/reset in `initWave()` and `respawnPlayer()`
3. Call `entity.update(dt, state)` in `GameLoop.tick()` under the `PLAYING` case
4. Add rendering in `EntityRenderer` or a new sub-renderer called from `GameRenderer.render()`
5. Add collision logic in `CollisionDetector.detect()`

### Adding a new shield mode

1. Add entry to `ShieldMode` enum
2. Add `rebuildXxx()` and `updateXxx()` methods to `Shield`
3. Add the new value to the `CYCLE` array in `WaveFactory`
4. Add cases in `GameState.initWave()` (rebuild dispatch) and `GameLoop.tick()` (update dispatch)

### Adding a new game mode

1. Add entry to `GameConfig.GameMode`
2. Add mode-specific branches in `CollisionDetector.detect()` (collision outcomes)
3. Add to `GameController.handleCannonFire()` and `AutoPilot` if fire conditions differ
4. Add to the config screen UI
