// rules.js — turn status: check / checkmate / stalemate / draws. Thin layer over
// attack + movegen. Insufficient-material is intentionally limited to King-vs-King:
// the 2D "bishop color complex" reasoning does NOT generalize to a 3D variant with
// planar + triagonal sliders, so we rely on the 50-move / repetition rules otherwise.
(function (NS) {
  var COLOR = NS.COLOR_BIT, PIECE_MASK = NS.PIECE_MASK, KING = NS.KING, WHITE = NS.WHITE;

  NS.isInCheck = function (state, color) {
    var kingIdx = (color === WHITE) ? state.whiteKing : state.blackKing;
    if (kingIdx < 0) return false;
    return NS.isAttacked(state.board, kingIdx, NS.opp(color));
  };

  NS.isCheckmate = function (state) {
    return NS.isInCheck(state, state.player) && !NS.hasAnyLegalMove(state);
  };

  NS.isStalemate = function (state) {
    return !NS.isInCheck(state, state.player) && !NS.hasAnyLegalMove(state);
  };

  NS.isFiftyMove = function (state) { return state.halfmove >= 100; };

  NS.insufficientMaterial = function (state) {
    var board = state.board;
    for (var i = 0; i < NS.CELLS; i++) {
      var t = board[i];
      if (t !== 0 && (t & PIECE_MASK) !== KING) return false; // any non-king piece => sufficient
    }
    return true; // only kings remain
  };

  // Composite status. repCount = how many times the current position has occurred
  // (the facade tracks this across the real game; pass 1 if not tracking).
  NS.status = function (state, repCount) {
    if (!NS.hasAnyLegalMove(state)) {
      return NS.isInCheck(state, state.player) ? 'checkmate' : 'stalemate';
    }
    if (NS.isFiftyMove(state)) return 'draw-50move';
    if (repCount >= 3) return 'draw-repetition';
    if (NS.insufficientMaterial(state)) return 'draw-insufficient';
    return NS.isInCheck(state, state.player) ? 'check' : 'playing';
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
