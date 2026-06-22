// agent-test.js — sanity checks for the AI search.
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
require('../src/agent.js');

var NS = globalThis.Chess3D;
var pass = 0, fail = 0;
function ok(n, c) { if (c) pass++; else { fail++; console.log('  FAIL ' + n); } }
var idx = NS.index;

// 1. from the start it returns a LEGAL move within reasonable time
(function () {
  var st = NS.initState(NS.buildStartBoard(), NS.WHITE);
  var legal = NS.generateLegalMoves(st);
  var t0 = Date.now();
  var m = NS.searchBestMove(st, { depth: 2, timeMs: 8000 });
  console.log('  start search depth 2: ' + (Date.now() - t0) + ' ms');
  ok('returns a move', m != null);
  ok('move is legal', legal.indexOf(m) >= 0);
  // search must leave the state untouched (balanced make/undo)
  ok('state restored after search', st.player === NS.WHITE && st.history.length === 0);
})();

// 2. grabs a free hanging queen
(function () {
  var b = NS.newBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.WHITE;
  b[idx(7, 7, 7)] = NS.KING | NS.BLACK;
  b[idx(4, 0, 4)] = NS.ROOK | NS.WHITE;   // rook on the file...
  b[idx(4, 4, 4)] = NS.QUEEN | NS.BLACK;  // ...with an undefended black queen ahead
  var st = NS.initState(b, NS.WHITE);
  var m = NS.searchBestMove(st, { depth: 2, timeMs: 8000 });
  ok('captures the hanging queen', m != null && NS.moveTarget(m) === idx(4, 4, 4));
})();

// 3. finds a mate-in-1 (rook delivers the cornered-king mate from one square away)
(function () {
  var b = NS.newBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.BLACK;
  b[idx(3, 5, 1)] = NS.KING | NS.WHITE;
  b[idx(0, 7, 0)] = NS.ROOK | NS.WHITE;
  b[idx(0, 0, 7)] = NS.ROOK | NS.WHITE;
  b[idx(7, 7, 0)] = NS.QUEEN | NS.WHITE;
  b[idx(7, 0, 7)] = NS.QUEEN | NS.WHITE;
  b[idx(0, 7, 7)] = NS.QUEEN | NS.WHITE;
  b[idx(7, 7, 7)] = NS.UNICORN | NS.WHITE;
  b[idx(7, 1, 0)] = NS.ROOK | NS.WHITE;   // this rook slides to (7,0,0) to cover (1,0,0) and check => mate
  var st = NS.initState(b, NS.WHITE);
  var m = NS.searchBestMove(st, { depth: 2, timeMs: 8000 });
  // apply the chosen move and confirm it is checkmate for black
  NS.makeMove(st, m);
  ok('chosen move delivers checkmate', NS.isCheckmate(st));
})();

console.log(pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
