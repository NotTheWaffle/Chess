// markers.js — all the visual feedback overlays: a selection cage around the picked
// piece, legal-move target markers (the ONLY pickable empties — phase-gated picking),
// a last-move indicator, and a check cage on the king. Target markers are spheres
// (orientation-free, unlike discs/rings) so they read from any orbit angle.
(function (NS) {
  var targetGroup, selBox, checkBox, lastFrom, lastTo;
  var targets = [];
  var quietMat, captureMat;

  function wireBox(color, size, opacity) {
    var box = new THREE.BoxGeometry(size, size, size);
    var edges = new THREE.EdgesGeometry(box);
    var ls = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: color, transparent: true, opacity: opacity, depthTest: true }));
    ls.visible = false;
    return ls;
  }

  function makeMarker(isCapture) {
    var r = isCapture ? 0.2 : 0.14;
    return new THREE.Mesh(new THREE.SphereGeometry(r, 14, 10), isCapture ? captureMat : quietMat);
  }

  NS.initMarkers = function () {
    quietMat = new THREE.MeshBasicMaterial({ color: 0x46e07a, transparent: true, opacity: 0.6, depthWrite: false });
    captureMat = new THREE.MeshBasicMaterial({ color: 0xff5a5a, transparent: true, opacity: 0.65, depthWrite: false });

    targetGroup = new THREE.Group(); targetGroup.name = 'targets';
    selBox = wireBox(0x35e0ff, 0.94, 0.95);
    checkBox = wireBox(0xff3b3b, 0.98, 0.95);
    lastFrom = wireBox(0xffc24d, 0.9, 0.32);
    lastTo = wireBox(0xffc24d, 0.92, 0.7);

    NS.scene.add(targetGroup, selBox, checkBox, lastFrom, lastTo);

    NS.markers = {
      targets: targets,
      showSelection: showSelection, clearSelection: clearSelection,
      showTargets: showTargets, clearTargets: clearTargets,
      showLastMove: showLastMove, clearLastMove: clearLastMove,
      showCheck: showCheck
    };
  };

  function showSelection(cellIndex) { NS.cellIndexToWorld(cellIndex, selBox.position); selBox.visible = true; NS.invalidate(); }
  function clearSelection() { selBox.visible = false; clearTargets(); NS.invalidate(); }

  function clearTargets() {
    for (var i = 0; i < targetGroup.children.length; i++) {
      var c = targetGroup.children[i];
      if (c.geometry) c.geometry.dispose();
    }
    targetGroup.clear();
    targets.length = 0;
    NS.invalidate();
  }

  // moves: flat array of move tokens (from the facade). Grouped by target cell so a
  // promotion cell (5 tokens) becomes one marker carrying all 5 choices.
  function showTargets(moves) {
    clearTargets();
    var byCell = {};
    moves.forEach(function (mv) { (byCell[mv.toIndex] || (byCell[mv.toIndex] = [])).push(mv); });
    Object.keys(byCell).forEach(function (ci) {
      ci = +ci;
      var list = byCell[ci];
      var marker = makeMarker(list[0].isCapture);
      NS.cellIndexToWorld(ci, marker.position);
      marker.userData = { isTarget: true, cell: ci, moves: list };
      targetGroup.add(marker);
      targets.push(marker);
    });
    NS.invalidate();
  }

  function showLastMove(token) {
    if (!token) { clearLastMove(); return; }
    NS.cellIndexToWorld(token.fromIndex, lastFrom.position); lastFrom.visible = true;
    NS.cellIndexToWorld(token.toIndex, lastTo.position); lastTo.visible = true;
    NS.invalidate();
  }
  function clearLastMove() { lastFrom.visible = false; lastTo.visible = false; NS.invalidate(); }

  function showCheck(cellIndex) {
    if (cellIndex == null || cellIndex < 0) { checkBox.visible = false; }
    else { NS.cellIndexToWorld(cellIndex, checkBox.position); checkBox.visible = true; }
    NS.invalidate();
  }
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
