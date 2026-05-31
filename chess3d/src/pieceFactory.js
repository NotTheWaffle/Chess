// pieceFactory.js — procedural piece meshes (no external 3D assets, so the game stays
// self-contained). Each piece type has a distinct primitive silhouette; the letter
// label removes any remaining ambiguity. Geometries (7) and materials (2, one per side)
// are SHARED across all ~256 pieces — only the lightweight Mesh objects are per-piece.
(function (NS) {
  var GEO = {}, matWhite, matBlack, built = false;

  function makeGeo(type) {
    switch (type) {
      case NS.PAWN:    return new THREE.SphereGeometry(0.22, 16, 12);
      case NS.ROOK:    return new THREE.BoxGeometry(0.42, 0.5, 0.42);
      case NS.KNIGHT:  return new THREE.TetrahedronGeometry(0.34);
      case NS.BISHOP:  return new THREE.ConeGeometry(0.26, 0.62, 18);
      case NS.QUEEN:   return new THREE.DodecahedronGeometry(0.32);
      case NS.KING:    return new THREE.CylinderGeometry(0.24, 0.30, 0.64, 18);
      case NS.UNICORN: return new THREE.OctahedronGeometry(0.34); // spiky = triagonal
    }
    return new THREE.SphereGeometry(0.2, 8, 6);
  }

  function build() {
    if (built) return;
    matWhite = new THREE.MeshStandardMaterial({ color: 0xEAD9A0, roughness: 0.5, metalness: 0.2, emissive: 0x2a2410, emissiveIntensity: 0.22 });
    matBlack = new THREE.MeshStandardMaterial({ color: 0xE0786E, roughness: 0.5, metalness: 0.2, emissive: 0x2a1410, emissiveIntensity: 0.22 });
    for (var t = NS.PAWN; t <= NS.UNICORN; t++) GEO[t] = makeGeo(t);
    built = true;
  }

  NS.pieceMaterial = function (side) { build(); return side === 'white' ? matWhite : matBlack; };

  NS.makePiece = function (type, side) {
    build();
    var mesh = new THREE.Mesh(GEO[type], side === 'white' ? matWhite : matBlack);
    mesh.userData = { isPiece: true, type: type, side: side, cell: -1 };
    mesh.add(NS.makeLabelSprite(type));
    return mesh;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
