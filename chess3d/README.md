# Chess 2.0 — 3D Volumetric Chess (8×8×8)

A massively expanded, truly three-dimensional chess variant: pieces move through a
**512-cell cube** rendered as a pannable 3D board with Three.js. Self-contained, no
build step, no server.

## Run it

**Double-click `index.html`.** It runs offline in any modern browser (Chrome, Edge,
Firefox). Everything — Three.js (vendored, r136) and all game code — loads via plain
`<script>` tags that work from `file://`.

- **Drag** to orbit · **right-drag** (or ctrl/shift-drag) to pan · **scroll** to zoom.
- Click one of your pieces to see its legal moves (green = move, red = capture), then
  click a marker to move. Click empty space to deselect.

## Rules

Coordinates are `(x, y, z)`, each `0..7`. `x` = files, `y` = ranks (the two armies face
each other along `y`), `z` = levels/height.

**Movement classes** (the 3D core): a direction is *orthogonal* (1 axis), *planar-
diagonal* (2 axes), or *triagonal* (3 axes — the space diagonal).

| Piece | Moves |
|-------|-------|
| **Rook (R)** | slides the 6 orthogonal directions |
| **Bishop (B)** | slides the 12 planar-diagonal directions |
| **Unicorn (U)** | slides the 8 triagonal directions — the new 3D piece |
| **Queen (Q)** | Rook + Bishop (18 directions) |
| **King (K)** | one step, any of 26 directions |
| **Knight (N)** | the 24 `{±2,±1,0}` leaps over the three axes |
| **Pawn (P)** | advances along `y`; captures the two forward planar-diagonals; double-steps from its start rank; **en passant**; **promotes** at the far `y`-face to Q/R/B/N/U |

Check, checkmate, stalemate, the 50-move rule, threefold repetition, and king-vs-king
draws are all enforced. **Castling is omitted** (ill-defined in 3D).

Each side starts with ~64 pieces on its lower four floors (z=0..3), leaving the upper
floors as open airspace.

## Controls (panel, top-right)

- **New Game / Undo**
- **Computer plays** White and/or Black, with a **Strength** (search depth) selector.
  The AI runs on the main thread (a "thinking…" overlay shows); depth 2 is fast, 3 is
  slow given the cube's huge branching factor.
- **View**: *Slice to levels* (show only a band of z-levels — the key tool for seeing
  into the cube), *Piece labels*, *Depth fog*.

## Architecture

Clean split between **engine** (pure logic, no DOM — also loadable in Node) and **view**
(Three.js). The view depends only on the `engine.js` facade.

```
engine:  constants, geometry, move, zobrist, board, setup,
         gamestate (make/undo), attack (isAttacked), movegen, rules, agent (AI), engine (facade)
view:    coords, scene, lattice, labels, pieceFactory, boardView, markers,
         visibility, picking, interaction, ui, ai
```

## Tests

Headless verification (no browser needed):

```bash
node test/engine-test.js       # piece moves, en passant, promotion, mate, make/undo, perft
node test/view-test.js         # boardView diff/pool sync (start, move, capture)
node test/interaction-test.js  # selection state machine vs the real engine
node test/agent-test.js        # AI returns legal moves, grabs material, finds mate-in-1
```

## Tunables

- **Starting army** — edit the `BACK` table (and `POPULATED_FLOORS`) in `src/setup.js`.
- **AI personality** — `evaluate()` and `CENTER_WEIGHT` in `src/agent.js`; piece values
  in `src/constants.js` (`PIECE_VALUES`).
