// app.js — bootstraps the game and wires the layers together.
(function (NS) {
  function boot() {
    try {
      var canvas = document.getElementById('gl');
      NS.initScene(canvas);
      NS.scene.add(NS.buildLattice());
      NS.initBoardView();
      NS.game = NS.createEngine();
      NS.boardView.syncToBoard(NS.game.getBoard());

      NS.initMarkers();
      NS.initUI();
      if (NS.initVisibility) NS.initVisibility(); // phase 5
      if (NS.initAI) NS.initAI();                 // phase 6
      NS.initInteraction();                       // also does the first refresh()
    } catch (err) {
      showError(err);
      throw err;
    }
  }

  function showError(err) {
    var el = document.getElementById('error');
    if (el) { el.style.display = 'block'; el.textContent = 'Error: ' + (err && err.stack ? err.stack : (err && err.message ? err.message : err)); }
  }
  window.addEventListener('error', function (e) { showError(e.error || e.message); });

  if (document.readyState !== 'loading') boot();
  else document.addEventListener('DOMContentLoaded', boot);
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
