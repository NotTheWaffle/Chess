// picking.js — turns clicks into game actions via raycasting. Two safeguards:
//  (1) click-vs-drag: a pointerup is only a "click" if the pointer barely moved and
//      was brief — otherwise it was an OrbitControls orbit/pan and we ignore it (else
//      every camera rotation would select a piece).
//  (2) phase-gated, occlusion-safe picking: if legal-move target markers exist (a piece
//      is selected) we raycast THOSE first; otherwise we raycast pieces. Nearest hit
//      wins, so an opaque near piece is never lost behind a far one.
(function (NS) {
  var raycaster, ndc, handlers, dom;
  var downX = 0, downY = 0, downT = 0, moved = false;
  var DRAG_PX = 6, CLICK_MS = 400;

  function now() { return (typeof performance !== 'undefined') ? performance.now() : Date.now(); }

  NS.initPicking = function (h) {
    handlers = h;
    raycaster = new THREE.Raycaster();
    ndc = new THREE.Vector2();
    dom = NS.renderer.domElement;
    dom.addEventListener('pointerdown', onDown);
    dom.addEventListener('pointermove', onMove);
    dom.addEventListener('pointerup', onUp);
  };

  function onDown(e) { downX = e.clientX; downY = e.clientY; downT = now(); moved = false; }
  function onMove(e) {
    if (!moved && (Math.abs(e.clientX - downX) > DRAG_PX || Math.abs(e.clientY - downY) > DRAG_PX)) moved = true;
  }
  function onUp(e) {
    if (moved || (now() - downT) > CLICK_MS) return; // it was a camera drag, not a click
    pick(e);
  }

  function pick(e) {
    var rect = dom.getBoundingClientRect();
    ndc.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    ndc.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
    raycaster.setFromCamera(ndc, NS.camera);

    var targets = NS.markers ? NS.markers.targets : [];
    if (targets.length) {
      var ht = raycaster.intersectObjects(targets, false);
      if (ht.length) { handlers.onTarget(ht[0].object); return; }
    }
    var pieces = NS.boardView ? NS.boardView.pieceMeshes : [];
    var hp = raycaster.intersectObjects(pieces, false); // non-recursive: skip label sprites
    if (hp.length) { handlers.onPiece(hp[0].object.userData.cell); return; }

    handlers.onEmpty();
  }
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
