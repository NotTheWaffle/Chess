// interaction-test.js — verifies the selection state machine (interaction.js) end-to-end
// against the REAL engine, stubbing only the view/UI/picking dependencies.
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
require('../src/engine.js');
require('../src/interaction.js');

var NS = globalThis.Chess3D;
var pass = 0, fail = 0;
function check(n, g, w) { if (g === w) pass++; else { fail++; console.log('  FAIL ' + n + ': got ' + g + ' want ' + w); } }
function ok(n, c) { if (c) pass++; else { fail++; console.log('  FAIL ' + n); } }
var idx = NS.index;

// ---- stubs that record interactions ----
var rec = { targets: null, lastMove: null, synced: 0, syncOpts: null, uiSide: null, uiStatus: null, promo: null, check: null };
NS.markers = {
  showSelection: function () {}, clearSelection: function () {},
  showTargets: function (m) { rec.targets = m; }, clearTargets: function () {},
  showLastMove: function (t) { rec.lastMove = t; }, clearLastMove: function () {},
  showCheck: function (c) { rec.check = c; }
};
NS.ui = { update: function (s, st) { rec.uiSide = s; rec.uiStatus = st; }, showPromoChooser: function (m) { rec.promo = m; } };
NS.boardView = { syncToBoard: function (b, o) { rec.synced++; rec.syncOpts = o; }, pieceMeshes: [], meshAt: [] };
var handlers;
NS.initPicking = function (h) { handlers = h; };

NS.game = NS.createEngine();
NS.initInteraction();
ok('picking wired', !!handlers);

// select a white pawn at (0,1,0): expect 2 legal targets (advance + double)
handlers.onPiece(idx(0, 1, 0));
check('selecting pawn shows its targets', rec.targets ? rec.targets.length : -1, 2);

// click the advance target (0,2,0): expect the move to apply and turn to flip to black
var advance = rec.targets.filter(function (m) { return m.toIndex === idx(0, 2, 0); })[0];
ok('advance target present', !!advance);
handlers.onTarget({ userData: { moves: [advance] } });
check('turn flipped to black', NS.game.getActiveSide(), 'black');
ok('board re-synced with animation', rec.synced > 0 && rec.syncOpts && rec.syncOpts.animate === advance);
ok('last move recorded', rec.lastMove === advance);
check('ui updated to black to move', rec.uiSide, 'black');

// clicking an opponent piece should NOT select (can't drive the other side)
rec.targets = null;
handlers.onPiece(idx(4, 0, 0)); // a white piece, but it's black's move now
ok('cannot select opponent piece', rec.targets === null);

// a target marker carrying 5 moves (a promotion cell) routes to the chooser, not applyMove
var sideBefore = NS.game.getActiveSide();
handlers.onTarget({ userData: { moves: [1, 2, 3, 4, 5] } });
ok('promotion routes to chooser', rec.promo && rec.promo.length === 5);
check('promotion chooser does not advance the game', NS.game.getActiveSide(), sideBefore);

console.log(pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
