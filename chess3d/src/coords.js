// coords.js — the single source of truth for mapping board (x,y,z) to world space.
//
// Board axes:  x = files, y = ranks (depth between the two players), z = levels (height).
// World axes (Three.js): X = right, Y = UP, Z = toward camera/depth.
// Mapping:  board-x -> world X,  board-z (height) -> world Y,  board-y (depth) -> world Z.
// The cube is centered on the origin: cell centers span -3.5..+3.5, boundaries -4..+4.
(function (NS) {
  var CELL = 1;                 // world units per cell
  NS.CELL = CELL;
  NS.HALF = (NS.SIZE - 1) / 2;  // 3.5 — offset that centers the cube on the origin
  NS.BOUND = (NS.SIZE * CELL) / 2; // 4 — half the cube's edge length

  // board (x,y,z) -> THREE.Vector3 world position (optionally reuse `target`)
  NS.cellToWorld = function (x, y, z, target) {
    var v = target || new THREE.Vector3();
    v.set((x - NS.HALF) * CELL, (z - NS.HALF) * CELL, (y - NS.HALF) * CELL);
    return v;
  };

  NS.cellIndexToWorld = function (i, target) {
    return NS.cellToWorld(NS.xOf(i), NS.yOf(i), NS.zOf(i), target);
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
