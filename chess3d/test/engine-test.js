// engine-test.js — headless (Node) verification of the 3D chess engine.
// The same src files load in the browser via <script>; here we require them in
// dependency order (each attaches to globalThis.Chess3D as a side effect).
require('../src/namespace.js');
require('../src/constants.js');
require('../src/geometry.js');
require('../src/move.js');
require('../src/zobrist.js');
require('../src/board.js');
require('../src/setup.js');
require('../src/gamestate.js');
require('../src/attack.js');
require('../src/movegen.js');
require('../src/rules.js');

var NS = globalThis.Chess3D;
var pass = 0, fail = 0;
function check(name, got, want) {
  if (got === want) { pass++; }
  else { fail++; console.log('  FAIL ' + name + ': got ' + got + ', want ' + want); }
}
function ok(name, cond) { if (cond) pass++; else { fail++; console.log('  FAIL ' + name); } }

// ---- helper: empty board with both kings tucked in non-interfering corners ----
function freshBoard() { return NS.newBoard(); }
function pseudoCount(board, cell) {
  var st = NS.initState(board, NS.WHITE);
  var out = []; NS.addPseudoMovesForCell(st, cell, out); return out.length;
}
var idx = NS.index;

console.log('== piece movement (hand-computed from center (3,3,3) on an empty board) ==');
(function () {
  var c = idx(3, 3, 3);
  var b;
  b = freshBoard(); b[c] = NS.ROOK | NS.WHITE;    check('rook',    pseudoCount(b, c), 21);
  b = freshBoard(); b[c] = NS.BISHOP | NS.WHITE;  check('bishop',  pseudoCount(b, c), 39);
  b = freshBoard(); b[c] = NS.UNICORN | NS.WHITE; check('unicorn', pseudoCount(b, c), 25);
  b = freshBoard(); b[c] = NS.QUEEN | NS.WHITE;   check('queen',   pseudoCount(b, c), 60);
  b = freshBoard(); b[c] = NS.KNIGHT | NS.WHITE;  check('knight',  pseudoCount(b, c), 24);
  b = freshBoard(); b[c] = NS.KING | NS.WHITE;    check('king',    pseudoCount(b, c), 26);
  // pawn: lone advance
  b = freshBoard(); b[c] = NS.PAWN | NS.WHITE;    check('pawn-advance', pseudoCount(b, c), 1);
  // pawn: from start rank => advance + double
  b = freshBoard(); b[idx(3, 1, 3)] = NS.PAWN | NS.WHITE; check('pawn-double', pseudoCount(b, idx(3, 1, 3)), 2);
  // pawn: advance + 2 diagonal captures
  b = freshBoard(); b[c] = NS.PAWN | NS.WHITE; b[idx(2, 4, 3)] = NS.PAWN | NS.BLACK; b[idx(4, 4, 3)] = NS.PAWN | NS.BLACK;
  check('pawn-captures', pseudoCount(b, c), 3);
})();

console.log('== en passant ==');
(function () {
  var b = freshBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.WHITE;     // tuck kings away
  b[idx(7, 7, 7)] = NS.KING | NS.BLACK;
  b[idx(4, 4, 2)] = NS.PAWN | NS.WHITE;     // white pawn ready to capture EP
  b[idx(5, 4, 2)] = NS.PAWN | NS.BLACK;     // black pawn that "just double-pushed" to y=4
  var st = NS.initState(b, NS.WHITE);
  st.ep = idx(5, 5, 2);                      // the passed-over cell (y=5)
  var moves = NS.legalMovesFrom(st, idx(4, 4, 2));
  var epMove = moves.filter(function (m) { return NS.moveFlag(m) === NS.FLAG_ENPASSANT; })[0];
  ok('ep-move-exists', epMove !== undefined);
  if (epMove) {
    NS.makeMove(st, epMove);
    ok('ep-captures-pawn', st.board[idx(5, 4, 2)] === 0);
    ok('ep-pawn-moved',    st.board[idx(5, 5, 2)] === (NS.PAWN | NS.WHITE));
    NS.undoMove(st);
    ok('ep-undo-restores-captured', st.board[idx(5, 4, 2)] === (NS.PAWN | NS.BLACK));
    ok('ep-undo-restores-mover',    st.board[idx(4, 4, 2)] === (NS.PAWN | NS.WHITE));
  }
})();

console.log('== promotion ==');
(function () {
  var b = freshBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.WHITE;
  b[idx(7, 7, 7)] = NS.KING | NS.BLACK;
  b[idx(3, 6, 2)] = NS.PAWN | NS.WHITE;     // one step from the last rank (y=7)
  var st = NS.initState(b, NS.WHITE);
  var moves = NS.legalMovesFrom(st, idx(3, 6, 2));
  check('promo-move-count', moves.length, 5); // Q R B N U
  var qPromo = moves.filter(function (m) { return NS.moveFlag(m) === NS.FLAG_PROMO && NS.movePromo(m) === NS.QUEEN; })[0];
  ok('promo-queen-exists', qPromo !== undefined);
  if (qPromo) { NS.makeMove(st, qPromo); ok('promo-places-queen', st.board[idx(3, 7, 2)] === (NS.QUEEN | NS.WHITE)); NS.undoMove(st); ok('promo-undo-restores-pawn', st.board[idx(3, 6, 2)] === (NS.PAWN | NS.WHITE)); }
})();

console.log('== constructed checkmate (black king cornered, all 7 escapes covered) ==');
(function () {
  var b = freshBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.BLACK;
  b[idx(3, 5, 1)] = NS.KING | NS.WHITE;     // off every attacking ray
  b[idx(7, 0, 0)] = NS.ROOK | NS.WHITE;     // covers (1,0,0) + checks
  b[idx(0, 7, 0)] = NS.ROOK | NS.WHITE;     // covers (0,1,0)
  b[idx(0, 0, 7)] = NS.ROOK | NS.WHITE;     // covers (0,0,1)
  b[idx(7, 7, 0)] = NS.QUEEN | NS.WHITE;    // covers (1,1,0)
  b[idx(7, 0, 7)] = NS.QUEEN | NS.WHITE;    // covers (1,0,1)
  b[idx(0, 7, 7)] = NS.QUEEN | NS.WHITE;    // covers (0,1,1)
  b[idx(7, 7, 7)] = NS.UNICORN | NS.WHITE;  // covers (1,1,1) triagonally
  var st = NS.initState(b, NS.BLACK);
  ok('mate-in-check', NS.isInCheck(st, NS.BLACK));
  ok('mate-no-legal-moves', !NS.hasAnyLegalMove(st));
  check('mate-status', NS.status(st, 1), 'checkmate');
})();

console.log('== make/undo invariant + incremental-hash correctness (random playouts) ==');
(function () {
  // deterministic-ish PRNG so reruns are stable
  var seed = 123456789;
  function rnd() { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; }
  var games = 40, maxPlies = 60, badRestore = 0, badHash = 0;
  for (var g = 0; g < games; g++) {
    var st = NS.initState();
    for (var p = 0; p < maxPlies; p++) {
      var moves = NS.generateLegalMoves(st);
      if (moves.length === 0) break;
      var m = moves[(rnd() * moves.length) | 0];
      // snapshot
      var snap = st.board.slice();
      var sPlayer = st.player, sEp = st.ep, sHalf = st.halfmove, sWK = st.whiteKing, sBK = st.blackKing, sH0 = st.hash0, sH1 = st.hash1;
      NS.makeMove(st, m);
      // incremental hash must match a full recompute
      var full = NS.computeHash(st.board, st.player, st.ep);
      if (full.h0 !== st.hash0 || full.h1 !== st.hash1) badHash++;
      NS.undoMove(st);
      // everything must be byte-for-byte restored
      var sameBoard = NS.boardsEqual(snap, st.board);
      if (!sameBoard || st.player !== sPlayer || st.ep !== sEp || st.halfmove !== sHalf ||
          st.whiteKing !== sWK || st.blackKing !== sBK || st.hash0 !== sH0 || st.hash1 !== sH1) badRestore++;
      // actually advance the game
      NS.makeMove(st, m);
    }
  }
  check('make/undo restores state', badRestore, 0);
  check('incremental hash matches recompute', badHash, 0);
})();

console.log('== perft (start position) + symmetry ==');
(function () {
  function perft(st, depth) {
    if (depth === 0) return 1;
    var moves = NS.generateLegalMoves(st), n = 0;
    for (var i = 0; i < moves.length; i++) { NS.makeMove(st, moves[i]); n += perft(st, depth - 1); NS.undoMove(st); }
    return n;
  }
  var white = NS.initState(NS.buildStartBoard(), NS.WHITE);
  var black = NS.initState(NS.buildStartBoard(), NS.BLACK);
  var p1w = NS.generateLegalMoves(white).length;
  var p1b = NS.generateLegalMoves(black).length;
  console.log('  perft(1) white = ' + p1w + ', black = ' + p1b);
  check('start position is symmetric (white moves == black moves)', p1w, p1b);
  var t0 = Date.now();
  var p2 = perft(white, 2);
  console.log('  perft(2) white = ' + p2 + '  (' + (Date.now() - t0) + ' ms)');
  ok('perft(1) > 0', p1w > 0);
  ok('perft(2) > 0', p2 > 0);
})();

console.log('\n' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
