// ai.js — drives the main-thread AI. Workers are blocked under file://, so the search
// runs on the main thread: we show the "thinking" overlay, render once, then yield a
// MACROTASK (setTimeout) so the overlay actually paints before the synchronous search
// blocks. (A microtask/Promise would run before paint.) This file is the single seam
// to later move the search into a Worker if the game is ever served over http://.
(function (NS) {
  NS.config = { aiWhite: false, aiBlack: false, aiDepth: 2 };

  function aiToMove() {
    var c = NS.config, side = NS.game.getActiveSide();
    return (c.aiWhite && side === 'white') || (c.aiBlack && side === 'black');
  }

  NS.maybeAiMove = function () {
    if (!aiToMove()) return;
    var status = NS.game.getStatus();
    if (status === 'checkmate' || status === 'stalemate' || status.indexOf('draw') === 0) return;

    NS.ui.setThinking(true);
    NS.invalidate();
    setTimeout(function () {
      var token = NS.game.searchBestMove({ depth: NS.config.aiDepth, timeMs: 8000 });
      NS.ui.setThinking(false);
      if (token) NS.interaction.applyMove(token); // applyMove re-invokes maybeAiMove (AI vs AI)
    }, 30);
  };

  NS.initAI = function () {
    var w = document.getElementById('ai-white'),
        b = document.getElementById('ai-black'),
        d = document.getElementById('ai-depth');
    function sync() { NS.config.aiWhite = w.checked; NS.config.aiBlack = b.checked; NS.config.aiDepth = +d.value; }
    w.addEventListener('change', function () { sync(); NS.maybeAiMove(); });
    b.addEventListener('change', function () { sync(); NS.maybeAiMove(); });
    d.addEventListener('change', sync);
    sync();
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
