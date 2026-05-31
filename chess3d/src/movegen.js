// movegen.js — pseudo-legal move generation per piece type, plus legality filtering
// (a move is legal iff it doesn't leave the mover's own king attacked). Ported from
// MoveHandler.java. Legality uses makeMove + isAttacked + undoMove. We deliberately
// filter only the moves of the piece being generated (not an accumulating list) to
// avoid the inherited O(n^2) re-scan bug.
(function (NS) {
  var COLOR = NS.COLOR_BIT, WHITE = NS.WHITE, PIECE_MASK = NS.PIECE_MASK;
  var PAWN = NS.PAWN, ROOK = NS.ROOK, KNIGHT = NS.KNIGHT, BISHOP = NS.BISHOP, QUEEN = NS.QUEEN, KING = NS.KING, UNICORN = NS.UNICORN;
  var ORTHO = NS.ORTHO, PLANAR = NS.PLANAR, TRIAGONAL = NS.TRIAGONAL, QUEEN_DIRS = NS.QUEEN_DIRS;
  var enc = NS.encodeMove;

  function pushPromos(out, origin, target) {
    for (var i = 0; i < NS.PROMO_OPTIONS.length; i++) out.push(enc(origin, target, NS.FLAG_PROMO, NS.PROMO_OPTIONS[i]));
  }

  function addSliderMoves(board, origin, dirs, color, out) {
    var x = NS.xOf(origin), y = NS.yOf(origin), z = NS.zOf(origin), i, d, cx, cy, cz, ti, t;
    for (i = 0; i < dirs.length; i++) {
      d = dirs[i]; cx = x + d[0]; cy = y + d[1]; cz = z + d[2];
      while (cx >= 0 && cx < 8 && cy >= 0 && cy < 8 && cz >= 0 && cz < 8) {
        ti = cx + (cy << 3) + (cz << 6); t = board[ti];
        if (t === 0) { out.push(enc(origin, ti, NS.FLAG_NORMAL, 0)); }
        else { if ((t & COLOR) !== color) out.push(enc(origin, ti, NS.FLAG_NORMAL, 0)); break; }
        cx += d[0]; cy += d[1]; cz += d[2];
      }
    }
  }

  function addLeaperMoves(board, origin, targets, color, out) {
    for (var i = 0; i < targets.length; i++) {
      var t = board[targets[i]];
      if (t === 0 || (t & COLOR) !== color) out.push(enc(origin, targets[i], NS.FLAG_NORMAL, 0));
    }
  }

  function addPawnMoves(state, cell, color, out) {
    var board = state.board, x = NS.xOf(cell), y = NS.yOf(cell), z = NS.zOf(cell);
    var fwdY = (color === WHITE) ? 1 : -1;
    var startRank = (color === WHITE) ? 1 : 6;
    var lastRank = (color === WHITE) ? 7 : 0;
    var ny = y + fwdY;
    if (ny < 0 || ny > 7) return; // pawns can't exist past the last rank, but guard anyway

    // quiet advance (+ double from start rank)
    var t1 = x + (ny << 3) + (z << 6);
    if (board[t1] === 0) {
      if (ny === lastRank) pushPromos(out, cell, t1);
      else {
        out.push(enc(cell, t1, NS.FLAG_NORMAL, 0));
        if (y === startRank) {
          var ny2 = y + 2 * fwdY, t2 = x + (ny2 << 3) + (z << 6);
          if (board[t2] === 0) out.push(enc(cell, t2, NS.FLAG_DOUBLE, 0));
        }
      }
    }

    // diagonal-forward captures (narrow rule: x±1, same z) + en passant
    var dxs = [-1, 1];
    for (var k = 0; k < 2; k++) {
      var nx = x + dxs[k];
      if (nx < 0 || nx > 7) continue;
      var tc = nx + (ny << 3) + (z << 6), tile = board[tc];
      if (tile !== 0 && (tile & COLOR) !== color) {
        if (ny === lastRank) pushPromos(out, cell, tc);
        else out.push(enc(cell, tc, NS.FLAG_NORMAL, 0));
      } else if (tile === 0 && tc === state.ep) {
        out.push(enc(cell, tc, NS.FLAG_ENPASSANT, 0));
      }
    }
  }

  NS.addPseudoMovesForCell = function (state, cell, out) {
    var board = state.board, piece = board[cell], type = piece & PIECE_MASK, color = piece & COLOR;
    switch (type) {
      case PAWN: addPawnMoves(state, cell, color, out); break;
      case ROOK: addSliderMoves(board, cell, ORTHO, color, out); break;
      case BISHOP: addSliderMoves(board, cell, PLANAR, color, out); break;
      case UNICORN: addSliderMoves(board, cell, TRIAGONAL, color, out); break;
      case QUEEN: addSliderMoves(board, cell, QUEEN_DIRS, color, out); break;
      case KNIGHT: addLeaperMoves(board, cell, NS.KNIGHT_TARGETS[cell], color, out); break;
      case KING: addLeaperMoves(board, cell, NS.KING_TARGETS[cell], color, out); break;
    }
  };

  // make the move, check the mover's king is safe, undo. (make-then-test ordering is
  // required so a king that slid along a check ray is judged from its new square.)
  NS.isLegalMove = function (state, move) {
    var mover = state.player;
    NS.makeMove(state, move);
    var kingIdx = (mover === WHITE) ? state.whiteKing : state.blackKing;
    var attacked = NS.isAttacked(state.board, kingIdx, state.player); // state.player now = opponent
    NS.undoMove(state);
    return !attacked;
  };

  NS.addLegalMovesForCell = function (state, cell, out) {
    var pseudo = [];
    NS.addPseudoMovesForCell(state, cell, pseudo);
    for (var i = 0; i < pseudo.length; i++) if (NS.isLegalMove(state, pseudo[i])) out.push(pseudo[i]);
  };

  NS.generateLegalMoves = function (state) {
    var board = state.board, out = [], color = state.player;
    for (var i = 0; i < NS.CELLS; i++) {
      var t = board[i];
      if (t !== 0 && (t & COLOR) === color) NS.addLegalMovesForCell(state, i, out);
    }
    return out;
  };

  // early-exit existence check (for checkmate/stalemate) — avoids building a full list
  NS.hasAnyLegalMove = function (state) {
    var board = state.board, color = state.player;
    for (var i = 0; i < NS.CELLS; i++) {
      var t = board[i];
      if (t === 0 || (t & COLOR) !== color) continue;
      var pseudo = [];
      NS.addPseudoMovesForCell(state, i, pseudo);
      for (var j = 0; j < pseudo.length; j++) if (NS.isLegalMove(state, pseudo[j])) return true;
    }
    return false;
  };

  NS.legalMovesFrom = function (state, cell) {
    var piece = state.board[cell];
    if (piece === 0 || (piece & COLOR) !== state.player) return [];
    var out = [];
    NS.addLegalMovesForCell(state, cell, out);
    return out;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
