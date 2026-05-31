// visibility.js — tools for seeing into a dense 512-cell cube. Slicing (show only a
// band of levels) is the highest-value tool, so it's the primary control. Also: a
// faint highlight plane on the active level, a label on/off toggle, and optional depth
// fog. applyVisibility() re-runs after every board sync (boardView calls it), because
// sync re-acquires meshes with visible=true.
(function (NS) {
  var sliceOn = false, level = 0, range = 0, labelsOn = true, fogOn = false;
  var levelPlane;

  function $(id) { return document.getElementById(id); }

  function inBand(z) { return !sliceOn || (z >= level - range && z <= level + range); }

  function apply() {
    var meshes = NS.boardView ? NS.boardView.pieceMeshes : [];
    for (var i = 0; i < meshes.length; i++) {
      var m = meshes[i], z = NS.zOf(m.userData.cell), vis = inBand(z);
      m.visible = vis;
      if (m.children[0]) m.children[0].visible = labelsOn; // label sprite (hidden anyway if parent hidden)
    }
    if (levelPlane) {
      levelPlane.visible = sliceOn;
      if (sliceOn) levelPlane.position.y = (level - NS.HALF) * NS.CELL;
    }
    NS.invalidate();
  }
  NS.applyVisibility = apply; // boardView calls this after each sync

  function setFog() {
    NS.scene.fog = fogOn ? new THREE.Fog(0x0a0d13, 10, 40) : null;
    NS.invalidate();
  }

  NS.initVisibility = function () {
    // faint highlight plane for the active level (world XZ plane at the level's height)
    var geo = new THREE.PlaneGeometry(NS.SIZE * NS.CELL, NS.SIZE * NS.CELL);
    geo.rotateX(-Math.PI / 2);
    levelPlane = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ color: 0x35e0ff, transparent: true, opacity: 0.07, depthWrite: false, side: THREE.DoubleSide }));
    levelPlane.visible = false;
    levelPlane.renderOrder = -1;
    NS.scene.add(levelPlane);

    var cb = $('view-slice'), lv = $('view-level'), rg = $('view-range'),
        lvVal = $('view-level-val'), rgVal = $('view-range-val'),
        lbl = $('view-labels'), fog = $('view-fog');

    cb.addEventListener('change', function () { sliceOn = cb.checked; apply(); });
    lv.addEventListener('input', function () { level = (+lv.value) - 1; lvVal.textContent = lv.value; apply(); });
    rg.addEventListener('input', function () { range = +rg.value; rgVal.textContent = rg.value; apply(); });
    lbl.addEventListener('change', function () { labelsOn = lbl.checked; apply(); });
    fog.addEventListener('change', function () { fogOn = fog.checked; setFog(); });

    // initialize from default control values
    sliceOn = cb.checked; level = (+lv.value) - 1; range = +rg.value; labelsOn = lbl.checked; fogOn = fog.checked;
    setFog();
    apply();
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
