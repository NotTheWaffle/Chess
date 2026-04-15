# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a bare Java project with no build tool (no Maven/Gradle). Requires Java 16+.

```bash
javac *.java
java Main
```

No external dependencies — only standard Java libraries (Swing/AWT).

There is no test framework. Manual testing is done via commented-out code in `Main.java`.

## Architecture

A ~1800-line Java chess engine with Swing GUI, playing at ~1500 ELO. All source files live in the project root (no packages).

### Layers

```
UI (Window, GamePanel, Input, Sprites)
  → Game Logic (ChessGame extends Game)
    → Move Generation & Validation (MoveHandler)
      → Board State (GameState, Board, Tile, Move)
        → AI Search (Agent, TranspositionTable)
```

### Board Representation

- `Board`: 64-element `byte[]` array. Index = `(y << 3) | x`, where (0,0) is bottom-left.
- `Tile`: Piece encoding via bit manipulation. Bits 0-2 = piece type (PAWN=1 through KING=6), bit 3 = color (WHITE=8, BLACK=0). E.g., WHITE_PAWN = 9 (0b1001).
- `Move`: 16-bit integer encoding: `flag(4) | target(6) | origin(6)`. Flags encode special moves (en passant, castling, promotion variants).
- `GameState`: Holds board, active player, castling rights, en passant index, king positions, and Zobrist hash. `makeMove()` applies moves and updates all state.

### Move Generation (MoveHandler)

The largest file (525 lines). Generates pseudo-legal moves per piece type, then filters out moves that leave the king in check. Key methods:
- `addLegalMoves()` / `addLegalMovesForTile()` — legal move generation
- `tryMove()` / `untryMove()` — push/pop moves on a stack (used by AI search)
- `makeMoveClone()` — returns new GameState after a move (non-destructive)
- `isAttacked(x, y, color)` — checks if a square is attacked by the opposing side

### AI Engine (Agent)

Negamax with alpha-beta pruning at fixed depth (default 4 ply). Move ordering sorts captures first. Uses `TranspositionTable` (Zobrist hash keyed) to cache search results.

Evaluation: material values (P=100, N=320, B=330, R=500, Q=900) plus piece-square tables for positional scoring. Separate king PSTs for early/late game.

Bot moves run on background threads via `ChessGame.callAgentPlay()`.

### UI (ChessGame, Window, GamePanel)

`ChessGame` orchestrates rendering and input. Click a piece to see legal moves, click a target to move. Supports board rotation and pawn promotion dialogs. `Sprites` loads PNG assets from `Assets/`.

## Key Design Decisions

- Move reversal via `tryMove`/`untryMove` stack vs. cloning (`makeMoveClone`) — both exist; the AI currently uses `makeMoveClone`.
- `-Integer.MIN_VALUE == Integer.MIN_VALUE` was a past bug source (see commit 06ab434). Checkmate score is -1,000,000 adjusted by depth, not `Integer.MIN_VALUE`.
- Game entry point in `Main.java`: `ChessGame(whiteBot, blackBot, rotate)` — booleans control human vs AI sides.

## Assets

- `Assets/Boards/{Clean,Crude,Indexed,RankFile}/` — board tile PNGs (light/dark)
- `Assets/Pieces/Clean/` — piece sprite PNGs (12 files: 6 piece types x 2 colors)
