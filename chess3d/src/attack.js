// attack.js — isAttacked(board, cell, byColor): does any `byColor` piece attack `cell`?
// We cast rays OUTWARD from the cell; the first occupied square on a ray is the only
// possible slider threat on that line (rays stop at first occupancy — never see through
// blockers). Threat-by-ray-class mapping:
//   orthogonal -> Rook/Queen,  planar -> Bishop/Queen,  triagonal -> Unicorn.
// Plus direct leaper/adjacency tests for Knight, King, and (narrow-rule) Pawn.
//
// CALLER CONTRACT (matches the Java engine): when testing the mover's king after a move,
// the move must already be applied to `board`, so a king that slid ALONG a check ray is
// evaluated from its NEW square with its origin vacated. Make-then-test, always.
(function (NS) {
  var PIECE_MASK = NS.PIECE_MASK, COLOR = NS.COLOR_BIT;
  var ROOK = NS.ROOK, KNIGHT = NS.KNIGHT, BISHOP = NS.BISHOP, QUEEN = NS.QUEEN, KING = NS.KING, UNICORN = NS.UNICORN, PAWN = NS.PAWN;
  var ORTHO = NS.ORTHO, PLANAR = NS.PLANAR, TRIAGONAL = NS.TRIAGONAL;
  var KNIGHT_TARGETS = NS.KNIGHT_TARGETS, KING_TARGETS = NS.KING_TARGETS;

  // first occupied tile along a ray from (x,y,z) stepping (dx,dy,dz); 0 if none
  function rayHit(board, x, y, z, dx, dy, dz) {
    x += dx; y += dy; z += dz;
    while (x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 8) {
      var t = board[x + (y << 3) + (z << 6)];
      if (t !== 0) return t;
      x += dx; y += dy; z += dz;
    }
    return 0;
  }

  NS.isAttacked = function (board, cell, byColor) {
    var x = NS.xOf(cell), y = NS.yOf(cell), z = NS.zOf(cell), i, d, t, type;

    // orthogonal rays: Rook or Queen
    for (i = 0; i < ORTHO.length; i++) {
      d = ORTHO[i]; t = rayHit(board, x, y, z, d[0], d[1], d[2]);
      if (t !== 0 && (t & COLOR) === byColor) { type = t & PIECE_MASK; if (type === ROOK || type === QUEEN) return true; }
    }
    // planar-diagonal rays: Bishop or Queen
    for (i = 0; i < PLANAR.length; i++) {
      d = PLANAR[i]; t = rayHit(board, x, y, z, d[0], d[1], d[2]);
      if (t !== 0 && (t & COLOR) === byColor) { type = t & PIECE_MASK; if (type === BISHOP || type === QUEEN) return true; }
    }
    // triagonal rays: Unicorn
    for (i = 0; i < TRIAGONAL.length; i++) {
      d = TRIAGONAL[i]; t = rayHit(board, x, y, z, d[0], d[1], d[2]);
      if (t !== 0 && (t & COLOR) === byColor && (t & PIECE_MASK) === UNICORN) return true;
    }
    // knight (symmetric: cells a knight attacks me from = cells I could jump to)
    var kt = KNIGHT_TARGETS[cell];
    for (i = 0; i < kt.length; i++) { t = board[kt[i]]; if (t !== 0 && (t & COLOR) === byColor && (t & PIECE_MASK) === KNIGHT) return true; }
    // king adjacency
    var gt = KING_TARGETS[cell];
    for (i = 0; i < gt.length; i++) { t = board[gt[i]]; if (t !== 0 && (t & COLOR) === byColor && (t & PIECE_MASK) === KING) return true; }
    // pawn (narrow rule): a byColor pawn one step "back" along its forward dir, offset ±1 in x, same z.
    // White pawns attack +y, so they sit at y-1 relative to `cell`; Black pawns sit at y+1.
    var py = (byColor === NS.WHITE) ? y - 1 : y + 1;
    if (py >= 0 && py < 8) {
      if (x - 1 >= 0) { t = board[(x - 1) + (py << 3) + (z << 6)]; if (t === (PAWN | byColor)) return true; }
      if (x + 1 < 8)  { t = board[(x + 1) + (py << 3) + (z << 6)]; if (t === (PAWN | byColor)) return true; }
    }
    return false;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
