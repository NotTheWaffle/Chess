// zobrist.js — Zobrist hashing for transposition table + repetition detection.
// JS bitwise ops are 32-bit, so a single 32-bit hash collides too often across a
// search of millions of nodes. We keep TWO independent 32-bit words (h0,h1) — an
// effective ~64-bit key — and the game maps positions by the "h0:h1" string.
// Keys are XORed in/out incrementally by gamestate.makeMove (never an O(512) rehash).
(function (NS) {
  var NTILE = 32; // tile codes range 0..24; pad to 32 for index math headroom
  var ZOB0 = new Int32Array(NTILE * NS.CELLS);
  var ZOB1 = new Int32Array(NTILE * NS.CELLS);
  var EPK0 = new Int32Array(NS.CELLS);
  var EPK1 = new Int32Array(NS.CELLS);
  var SIDE0, SIDE1;

  function r32() { return (Math.floor(Math.random() * 0x100000000)) | 0; }

  for (var t = 1; t < NTILE; t++) {           // skip tile 0 (empty) — never hashed
    for (var c = 0; c < NS.CELLS; c++) {
      ZOB0[t * NS.CELLS + c] = r32();
      ZOB1[t * NS.CELLS + c] = r32();
    }
  }
  for (var c2 = 0; c2 < NS.CELLS; c2++) { EPK0[c2] = r32(); EPK1[c2] = r32(); }
  SIDE0 = r32(); SIDE1 = r32();

  NS.zKey0 = function (tile, cell) { return ZOB0[tile * NS.CELLS + cell]; };
  NS.zKey1 = function (tile, cell) { return ZOB1[tile * NS.CELLS + cell]; };
  NS.zEp0 = function (cell) { return EPK0[cell]; };
  NS.zEp1 = function (cell) { return EPK1[cell]; };
  NS.zSide0 = SIDE0;
  NS.zSide1 = SIDE1;

  // Full (from-scratch) hash — used at init and as the perft verification oracle.
  // Convention: side keys are XORed in when it is BLACK to move.
  NS.computeHash = function (board, player, ep) {
    var h0 = 0, h1 = 0, i, tile;
    for (i = 0; i < NS.CELLS; i++) {
      tile = board[i];
      if (tile !== 0) { h0 ^= ZOB0[tile * NS.CELLS + i]; h1 ^= ZOB1[tile * NS.CELLS + i]; }
    }
    if (player === NS.BLACK) { h0 ^= SIDE0; h1 ^= SIDE1; }
    if (ep >= 0) { h0 ^= EPK0[ep]; h1 ^= EPK1[ep]; }
    return { h0: h0, h1: h1 };
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
