// view-test.js — headless validation of boardView's diff/pool sync logic (the riskiest
// view code) using minimal THREE + DOM stubs. Verifies piece counts, mesh placement,
// and capture handling without a real browser/WebGL.

// ---- minimal THREE / DOM stubs ----
function V3(x, y, z) { this.x = x || 0; this.y = y || 0; this.z = z || 0; }
V3.prototype.set = function (x, y, z) { this.x = x; this.y = y; this.z = z; return this; };
V3.prototype.copy = function (v) { this.x = v.x; this.y = v.y; this.z = v.z; return this; };
V3.prototype.clone = function () { return new V3(this.x, this.y, this.z); };
V3.prototype.lerpVectors = function (a, b, t) { this.x = a.x + (b.x - a.x) * t; this.y = a.y + (b.y - a.y) * t; this.z = a.z + (b.z - a.z) * t; return this; };
function Obj() { this.position = new V3(); this.scale = new V3(1, 1, 1); this.userData = {}; this.children = []; this.parent = null; this.visible = true; }
Obj.prototype.add = function (c) { this.children.push(c); c.parent = this; return this; };
function Mesh(g, m) { Obj.call(this); this.geometry = g; this.material = m; }
Mesh.prototype = Object.create(Obj.prototype);
function Sprite(m) { Obj.call(this); this.material = m; }
Sprite.prototype = Object.create(Obj.prototype);
function Group() { Obj.call(this); } Group.prototype = Object.create(Obj.prototype);
var geo = function () { return {}; };
globalThis.THREE = {
  Vector3: V3, Mesh: Mesh, Sprite: Sprite, Group: Group,
  SphereGeometry: geo, BoxGeometry: geo, TetrahedronGeometry: geo, ConeGeometry: geo,
  DodecahedronGeometry: geo, CylinderGeometry: geo, OctahedronGeometry: geo,
  MeshStandardMaterial: function () { return {}; },
  SpriteMaterial: function () { return {}; },
  CanvasTexture: function () { return {}; },
  LinearFilter: 1
};
var fakeCtx = new Proxy({}, { get: function () { return function () {}; } });
globalThis.document = { createElement: function () { return { getContext: function () { return fakeCtx; } }; } };
globalThis.performance = { now: function () { return 0; } };

// ---- load engine + the view pieces we can exercise ----
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
require('../src/coords.js');
require('../src/labels.js');
require('../src/pieceFactory.js');
require('../src/boardView.js');

var NS = globalThis.Chess3D;
NS.scene = new Group();
NS.invalidate = function () {};
NS.onEachFrame = function () {}; // provided by scene.js in the browser
NS.initBoardView();

var pass = 0, fail = 0;
function check(name, got, want) { if (got === want) pass++; else { fail++; console.log('  FAIL ' + name + ': got ' + got + ', want ' + want); } }
function ok(name, c) { if (c) pass++; else { fail++; console.log('  FAIL ' + name); } }
var idx = NS.index, bv = NS.boardView;

// 1. starting position renders the right number of pieces (64/side = 128 total)
var eng = NS.createEngine();
bv.syncToBoard(eng.getBoard());
check('start: total piece meshes', bv.pieceMeshes.length, 128);
ok('start: white king mesh present', bv.meshAt[idx(4, 0, 0)] && bv.meshAt[idx(4, 0, 0)].userData.type === NS.KING && bv.meshAt[idx(4, 0, 0)].userData.side === 'white');

// 2. a quiet move: pawn (0,1,0) -> (0,2,0)
(function () {
  var token = eng.legalMovesFrom([0, 1, 0]).filter(function (m) { return m.toIndex === idx(0, 2, 0); })[0];
  ok('quiet move token exists', !!token);
  eng.makeMove(token);
  bv.syncToBoard(eng.getBoard(), { animate: token });
  check('after move: count unchanged', bv.pieceMeshes.length, 128);
  ok('after move: origin empty', bv.meshAt[idx(0, 1, 0)] === null);
  ok('after move: target has pawn', bv.meshAt[idx(0, 2, 0)] && bv.meshAt[idx(0, 2, 0)].userData.type === NS.PAWN);
})();

// 3. a capture on a crafted board reduces the mesh count by one
(function () {
  var b = NS.newBoard();
  b[idx(0, 0, 0)] = NS.KING | NS.WHITE;
  b[idx(7, 7, 7)] = NS.KING | NS.BLACK;
  b[idx(3, 3, 3)] = NS.PAWN | NS.WHITE;
  b[idx(4, 4, 3)] = NS.PAWN | NS.BLACK; // capturable diagonally-forward
  var st = NS.initState(b, NS.WHITE);
  // drive boardView from this custom board directly
  bv.syncToBoard(st.board);
  var before = bv.pieceMeshes.length; // 4 (2 kings + 2 pawns)
  check('crafted: 4 pieces', before, 4);
  var caps = NS.legalMovesFrom(st, idx(3, 3, 3)).filter(function (m) { return NS.moveTarget(m) === idx(4, 4, 3); });
  ok('crafted: capture available', caps.length === 1);
  NS.makeMove(st, caps[0]);
  bv.syncToBoard(st.board);
  check('crafted: capture removed a piece', bv.pieceMeshes.length, 3);
  ok('crafted: capturer at target', bv.meshAt[idx(4, 4, 3)] && bv.meshAt[idx(4, 4, 3)].userData.side === 'white');
  ok('crafted: origin empty', bv.meshAt[idx(3, 3, 3)] === null);
})();

console.log(pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
