// ui.js — HTML/CSS overlay behavior: turn/status HUD, end-game banner, promotion
// chooser, "thinking" overlay, and the New Game / Undo controls. The static structure
// lives in index.html; this file sets text/visibility and wires the buttons it owns.
// (View controls are wired by visibility.js; AI controls by ai.js — each owns its own.)
(function (NS) {
  var hudText, banner, bannerText, promo, promoButtons, thinking;

  function $(id) { return document.getElementById(id); }
  function cap(s) { return s.charAt(0).toUpperCase() + s.slice(1); }
  function other(side) { return side === 'white' ? 'black' : 'white'; }
  function clear(el) { while (el.firstChild) el.removeChild(el.firstChild); }

  var PROMO_NAMES = {}; // type -> full name
  PROMO_NAMES[NS.QUEEN] = 'Queen'; PROMO_NAMES[NS.ROOK] = 'Rook'; PROMO_NAMES[NS.BISHOP] = 'Bishop';
  PROMO_NAMES[NS.KNIGHT] = 'Knight'; PROMO_NAMES[NS.UNICORN] = 'Unicorn';

  function update(side, status) {
    var label;
    switch (status) {
      case 'check': label = cap(side) + ' to move — CHECK'; break;
      case 'checkmate': label = 'Checkmate — ' + cap(other(side)) + ' wins'; break;
      case 'stalemate': label = 'Stalemate — draw'; break;
      case 'draw-50move': label = 'Draw — 50-move rule'; break;
      case 'draw-repetition': label = 'Draw — threefold repetition'; break;
      case 'draw-insufficient': label = 'Draw — insufficient material'; break;
      default: label = cap(side) + ' to move';
    }
    hudText.textContent = label;
    hudText.className = (side === 'white') ? 'turn-white' : 'turn-black';
    var terminal = (status !== 'playing' && status !== 'check');
    if (terminal) showBanner(label); else hideBanner();
  }

  function showBanner(text) { bannerText.textContent = text; banner.style.display = 'flex'; }
  function hideBanner() { banner.style.display = 'none'; }
  function setThinking(on) { thinking.style.display = on ? 'flex' : 'none'; }

  function showPromoChooser(moves, cb) {
    clear(promoButtons);
    moves.forEach(function (mv) {
      var b = document.createElement('button');
      b.textContent = NS.PIECE_LETTERS[mv.promo] + ' · ' + PROMO_NAMES[mv.promo];
      b.onclick = function () { promo.style.display = 'none'; cb(mv); };
      promoButtons.appendChild(b);
    });
    promo.style.display = 'flex';
  }

  function newGame() {
    NS.game.newGame();
    NS.boardView.syncToBoard(NS.game.getBoard());
    NS.markers.clearSelection();
    NS.markers.clearLastMove();
    NS.markers.showCheck(-1);
    hideBanner();
    NS.interaction.refresh();
    if (NS.maybeAiMove) NS.maybeAiMove();
  }

  function undo() {
    // when playing vs AI, undo both the AI reply and your move so it's your turn again
    var c = NS.config || {};
    var vsAI = c.aiWhite || c.aiBlack;
    if (!NS.game.undo()) return;
    if (vsAI && NS.game.canUndo()) NS.game.undo();
    NS.boardView.syncToBoard(NS.game.getBoard());
    NS.markers.clearSelection();
    NS.markers.showLastMove(NS.game.getLastMove());
    hideBanner();
    NS.interaction.refresh();
  }

  NS.initUI = function () {
    hudText = $('hud-text');
    banner = $('banner'); bannerText = $('banner-text');
    promo = $('promo'); promoButtons = $('promo-buttons');
    thinking = $('thinking');
    $('btn-newgame').addEventListener('click', newGame);
    $('btn-undo').addEventListener('click', undo);
    $('banner-newgame').addEventListener('click', newGame);
    NS.ui = { update: update, showBanner: showBanner, hideBanner: hideBanner, setThinking: setThinking, showPromoChooser: showPromoChooser, newGame: newGame, undo: undo };
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
