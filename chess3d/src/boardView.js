// boardView.js — keeps the 3D pieces in sync with the engine board. syncToBoard() is
// authoritative: it diffs the desired board against the live meshes and reconciles via
// an object pool (release mismatches, acquire/reposition where needed). This single
// uniform diff handles moves, captures, en-passant, promotion, undo and new-game with
// no special cases. opts.animate (a move token) slides the moved piece for polish.
(function (NS) {
  var meshAt = new Array(NS.CELLS);
  var pool = {};            // 'type_side' -> [free meshes]
  var pieceMeshes = [];     // active meshes (the raycast set for picking)
  var tweens = [];
  var group, tmp = new THREE.Vector3(), tmp2 = new THREE.Vector3();

  function key(type, side) { return type + '_' + side; }

  function acquire(type, side) {
    var arr = pool[key(type, side)];
    var m = (arr && arr.length) ? arr.pop() : NS.makePiece(type, side);
    m.visible = true;
    return m;
  }
  function release(m) {
    (pool[key(m.userData.type, m.userData.side)] || (pool[key(m.userData.type, m.userData.side)] = [])).push(m);
    m.visible = false;
    m.userData.cell = -1;
  }

  function rebuildPieceList() {
    pieceMeshes.length = 0;
    for (var i = 0; i < NS.CELLS; i++) if (meshAt[i]) pieceMeshes.push(meshAt[i]);
  }

  function syncToBoard(board, opts) {
    opts = opts || {};
    var i, tile, m, wantType, wantSide;
    // 1. release meshes that no longer match their cell
    for (i = 0; i < NS.CELLS; i++) {
      m = meshAt[i]; if (!m) continue;
      tile = board[i];
      wantType = tile ? (tile & NS.PIECE_MASK) : 0;
      wantSide = tile ? ((tile & NS.COLOR_BIT) ? 'white' : 'black') : null;
      if (!tile || m.userData.type !== wantType || m.userData.side !== wantSide) { release(m); meshAt[i] = null; }
    }
    // 2. acquire/reposition meshes where the board has a piece
    for (i = 0; i < NS.CELLS; i++) {
      tile = board[i]; if (!tile) continue;
      wantType = tile & NS.PIECE_MASK; wantSide = (tile & NS.COLOR_BIT) ? 'white' : 'black';
      m = meshAt[i];
      if (!m) {
        m = acquire(wantType, wantSide);
        m.userData.cell = i; m.userData.type = wantType; m.userData.side = wantSide;
        if (!m.parent) group.add(m);
        meshAt[i] = m;
      }
      NS.cellIndexToWorld(i, m.position);
      m.userData.cell = i;
    }
    rebuildPieceList();
    if (NS.applyVisibility) NS.applyVisibility(); // re-apply slicing/labels after re-acquire

    // 3. optional slide animation for the piece that just moved
    if (opts.animate) {
      var tok = opts.animate, mm = meshAt[tok.toIndex];
      if (mm) {
        NS.cellIndexToWorld(tok.fromIndex, tmp);
        NS.cellIndexToWorld(tok.toIndex, tmp2);
        startTween(mm, tmp.clone(), tmp2.clone());
      }
    }
    NS.invalidate();
  }

  function startTween(mesh, fromPos, toPos) {
    mesh.position.copy(fromPos);
    var now = (typeof performance !== 'undefined') ? performance.now() : Date.now();
    tweens.push({ mesh: mesh, from: fromPos, to: toPos, start: now, dur: 220 });
  }

  function updateTweens(now) {
    if (tweens.length === 0) return;
    for (var i = tweens.length - 1; i >= 0; i--) {
      var tw = tweens[i], p = (now - tw.start) / tw.dur;
      if (p >= 1) { tw.mesh.position.copy(tw.to); tweens.splice(i, 1); }
      else {
        var e = p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2; // easeInOutQuad
        tw.mesh.position.lerpVectors(tw.from, tw.to, e);
      }
    }
    NS.invalidate();
  }

  NS.initBoardView = function () {
    group = new THREE.Group();
    group.name = 'pieces';
    NS.scene.add(group);
    NS.onEachFrame(updateTweens);
    NS.boardView = {
      group: group,
      pieceMeshes: pieceMeshes,
      meshAt: meshAt,
      syncToBoard: syncToBoard
    };
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
