// interaction.js — the selection state machine that binds picking <-> engine <-> view.
//   idle --click own piece--> selected (show legal targets)
//   selected --click target--> apply move (or open promotion chooser) --> idle
//   selected --click own piece--> reselect ;  --click empty/opponent--> idle
(function (NS) {
  var game, selectedCell = -1;

  function activeSide() { return game.getActiveSide(); }

  // AI config (set by phase 6); default both sides human.
  function isHumanTurn() {
    var c = NS.config || {};
    if (c.aiWhite && activeSide() === 'white') return false;
    if (c.aiBlack && activeSide() === 'black') return false;
    return true;
  }

  function deselect() { selectedCell = -1; NS.markers.clearSelection(); }

  function onPiece(cell) {
    if (!isHumanTurn()) return;
    var tile = game.getBoard()[cell];
    if (!tile) { deselect(); return; }
    var side = (tile & NS.COLOR_BIT) ? 'white' : 'black';
    if (side !== activeSide()) { deselect(); return; } // can't drive the opponent
    selectedCell = cell;
    NS.markers.showSelection(cell);
    NS.markers.showTargets(game.legalMovesFrom([NS.xOf(cell), NS.yOf(cell), NS.zOf(cell)]));
  }

  function onTarget(marker) {
    var moves = marker.userData.moves;
    if (moves.length === 1) applyMove(moves[0]);
    else NS.ui.showPromoChooser(moves, applyMove); // promotion: same from/to, 5 choices
  }

  function applyMove(token) {
    deselect();
    game.makeMove(token);
    NS.boardView.syncToBoard(game.getBoard(), { animate: token });
    NS.markers.showLastMove(token);
    refresh();
    if (NS.maybeAiMove) NS.maybeAiMove(); // phase 6 hook
  }

  function refresh() {
    var status = game.getStatus(), side = activeSide();
    var inCheck = (status === 'check' || status === 'checkmate');
    var kc = inCheck ? game.getKingCell(side) : null;
    NS.markers.showCheck(kc ? NS.index(kc[0], kc[1], kc[2]) : -1);
    if (NS.ui) NS.ui.update(side, status);
  }

  NS.initInteraction = function () {
    game = NS.game;
    selectedCell = -1;
    NS.initPicking({ onPiece: onPiece, onTarget: onTarget, onEmpty: deselect });
    NS.interaction = { applyMove: applyMove, refresh: refresh, deselect: deselect, isHumanTurn: isHumanTurn };
    refresh();
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
