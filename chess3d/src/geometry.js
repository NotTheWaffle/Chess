// geometry.js — the heart of 3D movement. Direction vectors are GENERATED (not
// hardcoded) by grouping every (dx,dy,dz) in {-1,0,1}^3 (minus the zero vector) by
// how many coordinates are nonzero: 1 -> orthogonal (Rook), 2 -> planar-diagonal
// (Bishop), 3 -> triagonal (Unicorn). Counts are asserted (6/12/8) as a sanity gate.
// Per-cell knight & king target lists are precomputed (bounds-clipped) so move-gen
// and isAttacked never redo bounds arithmetic.
(function (NS) {
  var ORTHO = [], PLANAR = [], TRIAGONAL = [];
  for (var dx = -1; dx <= 1; dx++)
    for (var dy = -1; dy <= 1; dy++)
      for (var dz = -1; dz <= 1; dz++) {
        var n = (dx ? 1 : 0) + (dy ? 1 : 0) + (dz ? 1 : 0);
        if (n === 1) ORTHO.push([dx, dy, dz]);
        else if (n === 2) PLANAR.push([dx, dy, dz]);
        else if (n === 3) TRIAGONAL.push([dx, dy, dz]);
      }

  function assertCount(name, arr, expected) {
    if (arr.length !== expected) throw new Error('geometry: ' + name + ' has ' + arr.length + ', expected ' + expected);
  }
  assertCount('ORTHO', ORTHO, 6);
  assertCount('PLANAR', PLANAR, 12);
  assertCount('TRIAGONAL', TRIAGONAL, 8);

  NS.ORTHO = ORTHO;
  NS.PLANAR = PLANAR;
  NS.TRIAGONAL = TRIAGONAL;
  NS.QUEEN_DIRS = ORTHO.concat(PLANAR);          // 18
  NS.KING_DIRS = ORTHO.concat(PLANAR, TRIAGONAL); // 26
  assertCount('QUEEN_DIRS', NS.QUEEN_DIRS, 18);
  assertCount('KING_DIRS', NS.KING_DIRS, 26);

  // Knight offsets: the {±2, ±1, 0} pattern over the 3 axes (abs values are {0,1,2}).
  var KNIGHT_OFFSETS = [];
  for (var a = -2; a <= 2; a++)
    for (var b = -2; b <= 2; b++)
      for (var c = -2; c <= 2; c++) {
        var s = [Math.abs(a), Math.abs(b), Math.abs(c)].sort();
        if (s[0] === 0 && s[1] === 1 && s[2] === 2) KNIGHT_OFFSETS.push([a, b, c]);
      }
  assertCount('KNIGHT_OFFSETS', KNIGHT_OFFSETS, 24);
  NS.KNIGHT_OFFSETS = KNIGHT_OFFSETS;

  // Precompute per-cell, bounds-clipped target index lists for the two leapers.
  var KNIGHT_TARGETS = new Array(NS.CELLS);
  var KING_TARGETS = new Array(NS.CELLS);
  for (var i = 0; i < NS.CELLS; i++) {
    var x = NS.xOf(i), y = NS.yOf(i), z = NS.zOf(i), j, off, nx, ny, nz, list;
    list = [];
    for (j = 0; j < KNIGHT_OFFSETS.length; j++) {
      off = KNIGHT_OFFSETS[j]; nx = x + off[0]; ny = y + off[1]; nz = z + off[2];
      if (NS.inBounds(nx, ny, nz)) list.push(NS.index(nx, ny, nz));
    }
    KNIGHT_TARGETS[i] = list;
    list = [];
    for (j = 0; j < NS.KING_DIRS.length; j++) {
      off = NS.KING_DIRS[j]; nx = x + off[0]; ny = y + off[1]; nz = z + off[2];
      if (NS.inBounds(nx, ny, nz)) list.push(NS.index(nx, ny, nz));
    }
    KING_TARGETS[i] = list;
  }
  NS.KNIGHT_TARGETS = KNIGHT_TARGETS;
  NS.KING_TARGETS = KING_TARGETS;
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
