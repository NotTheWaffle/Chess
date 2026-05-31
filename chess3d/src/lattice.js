// lattice.js — the see-through cube structure. Empty space must READ as empty, so
// we never draw solid boxes for empty cells. Instead: faint grid lines on the 6
// outer faces (one draw call) + a brighter bounding box silhouette. The interior
// stays open so pieces are visible floating inside a "gridded room".
(function (NS) {
  function boundaries() {
    var b = [], B = NS.BOUND, i;
    for (i = 0; i <= NS.SIZE; i++) b.push(-B + i * NS.CELL); // [-4 .. +4], 9 values
    return b;
  }

  function faceGridPositions() {
    var B = NS.BOUND, bs = boundaries(), pos = [], i, a;
    function seg(x1, y1, z1, x2, y2, z2) { pos.push(x1, y1, z1, x2, y2, z2); }
    [-B, B].forEach(function (fx) { // faces perpendicular to X (Y-Z plane)
      for (i = 0; i < bs.length; i++) { a = bs[i]; seg(fx, a, -B, fx, a, B); seg(fx, -B, a, fx, B, a); }
    });
    [-B, B].forEach(function (fy) { // faces perpendicular to Y (X-Z plane)
      for (i = 0; i < bs.length; i++) { a = bs[i]; seg(a, fy, -B, a, fy, B); seg(-B, fy, a, B, fy, a); }
    });
    [-B, B].forEach(function (fz) { // faces perpendicular to Z (X-Y plane)
      for (i = 0; i < bs.length; i++) { a = bs[i]; seg(a, -B, fz, a, B, fz); seg(-B, a, fz, B, a, fz); }
    });
    return pos;
  }

  function boxEdgePositions() {
    var B = NS.BOUND, p = [];
    var v = [[-B, -B, -B], [B, -B, -B], [B, B, -B], [-B, B, -B],
             [-B, -B, B], [B, -B, B], [B, B, B], [-B, B, B]];
    var e = [[0, 1], [1, 2], [2, 3], [3, 0], [4, 5], [5, 6], [6, 7], [7, 4], [0, 4], [1, 5], [2, 6], [3, 7]];
    e.forEach(function (pair) { var a = v[pair[0]], b = v[pair[1]]; p.push(a[0], a[1], a[2], b[0], b[1], b[2]); });
    return p;
  }

  NS.buildLattice = function () {
    var group = new THREE.Group();
    group.name = 'lattice';

    var gGeo = new THREE.BufferGeometry();
    gGeo.setAttribute('position', new THREE.Float32BufferAttribute(faceGridPositions(), 3));
    var gMat = new THREE.LineBasicMaterial({ color: 0x39496a, transparent: true, opacity: 0.22, depthWrite: false });
    var grid = new THREE.LineSegments(gGeo, gMat);
    grid.renderOrder = -2;
    group.add(grid);

    var bGeo = new THREE.BufferGeometry();
    bGeo.setAttribute('position', new THREE.Float32BufferAttribute(boxEdgePositions(), 3));
    var bMat = new THREE.LineBasicMaterial({ color: 0x7088bc, transparent: true, opacity: 0.6, depthWrite: false });
    var box = new THREE.LineSegments(bGeo, bMat);
    box.renderOrder = -1;
    group.add(box);

    NS.lattice = { group: group, gridMat: gMat, boxMat: bMat };
    return group;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
