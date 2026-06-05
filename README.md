# Yars' Revenge Remake (2026)

A JavaFX remake of the Atari 2600 classic, built by vibe-coding.

## Requirements

- Java 21+
- Maven 3.8+

## Run

```
mvn javafx:run
```

## Build fat JAR

```
mvn package
```

## Controls

| Key | Action |
|-----|--------|
| Arrow keys / WASD | Move |
| Enter | Fire bullet |
| Space | Fire Zorlon Cannon (when charged) |
| Esc | Pause |
| M | Minimize window |

## How to play

1. Use the Torpedo (Orb) or your bullets to eat through the shield.
2. Expose the Quotile (enemy), then fire the Zorlon Cannon to destroy it.
3. Avoid Quotile shots, the Torpedo, and the homing Quotile missile.

## Scoring

| Action | Points |
|--------|--------|
| Shield cell eaten | 10 |
| Torpedo (Orb) destroyed | 500 |
| Quotile destroyed | 1000 + wave × 200 |

## Wave behaviour

| Wave | Shield type |
|------|-------------|
| Odd  | Arc shield — polar grid orbiting the Quotile, static |
| Even | Rect shield — flat grid wall left of Quotile, cells scroll right→down in a loop, wall tracks Quotile Y |

## Architecture

```
com.yarsrevenge.app        — entry point, SceneManager (single Stage/Scene, swap roots)
com.yarsrevenge.model      — GameState, GameConstants, entities, wave config
com.yarsrevenge.controller — InputHandler (keyboard), GameController
com.yarsrevenge.engine     — GameLoop (AnimationTimer, 60 Hz fixed timestep), CollisionDetector
com.yarsrevenge.renderer   — GameRenderer (gc.scale to physical size), sub-renderers, RenderContext
com.yarsrevenge.audio      — PcmSynthesizer (WAV bytes generated in code), AudioManager (temp files)
com.yarsrevenge.screen     — Screen interface + MainMenu, Game, GameOver, HighScore, Instructions
```

## Key design decisions

- **No asset files** — all audio synthesized at runtime via `PcmSynthesizer`, written to temp WAV files (JavaFX `AudioClip` does not support data: URIs).
- **Stretch-to-fill rendering** — all game logic in 1920×1080 logical space; `gc.scale()` maps to physical window.
- **Fixed timestep** — simulation runs at 60 Hz regardless of render FPS.
- **High scores** — stored in `~/.yars-revenge-scores` (plaintext, one score per line).
