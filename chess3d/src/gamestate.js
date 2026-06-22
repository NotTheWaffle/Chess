// gamestate.js — mutable game state + reversible makeMove/undoMove (ported from the
// Java GameState/MoveHandler try/untry pattern, minus castling). makeMove updates the
// Zobrist hash INCREMENTALLY (no O(512) rehash in the search hot path); undoMove simply
// restores the previous hash saved in the undo record, so a make-side hash bug can never
// desync across undo (and perft asserts the incremental hash matches a full recompute).
(function (NS) {
  var COLOR = NS.COLOR_BIT, WHITE = NS.WHITE, KING = NS.KING, PAWN = NS.PAWN;

  NS.scanKings = function (board) {
    var wk = -1, bk = -1;
    for (var i = 0; i < NS.CELLS; i++) {
      if ((board[i] & NS.PIECE_MASK) === KING) {
        if ((board[i] & COLOR) === WHITE) wk = i; else bk = i;
      }
    }
    return { whiteKing: wk, blackKing: bk };
  };

  NS.initState = function (board, player) {
    board = board || NS.buildStartBoard();
    player = (player === undefined) ? WHITE : player;
    var kings = NS.scanKings(board);
    var h = NS.computeHash(board, player, -1);
    return {
      board: board,
      player: player,
      ep: -1,
      halfmove: 0,
      whiteKing: kings.whiteKing,
      blackKing: kings.blackKing,
      hash0: h.h0,
      hash1: h.h1,
      history: []
    };
  };

  NS.makeMove = function (state, move) {
    var board = state.board;
    var origin = NS.moveOrigin(move), target = NS.moveTarget(move);
    var flag = NS.moveFlag(move), promo = NS.movePromo(move);
    var piece = board[origin];
    var color = piece & COLOR;
    var ptype = piece & NS.PIECE_MASK;
    var fwd = NS.forwardStep(color);

    var captured, epCapCell = -1;
    if (flag === NS.FLAG_ENPASSANT) { epCapCell = target - fwd; captured = board[epCapCell]; }
    else captured = board[target];

    state.history.push({
      move: move, captured: captured, epCapCell: epCapCell,
      prevEp: state.ep, prevHalfmove: state.halfmove,
      prevWK: state.whiteKing, prevBK: state.blackKing,
      prevH0: state.hash0, prevH1: state.hash1
    });

    // remove the old en-passant target from the hash (a new one is re-added below)
    if (state.ep >= 0) { state.hash0 ^= NS.zEp0(state.ep); state.hash1 ^= NS.zEp1(state.ep); }

    // remove captured piece
    if (flag === NS.FLAG_ENPASSANT) {
      state.hash0 ^= NS.zKey0(captured, epCapCell); state.hash1 ^= NS.zKey1(captured, epCapCell);
      board[epCapCell] = 0;
    } else if (captured !== 0) {
      state.hash0 ^= NS.zKey0(captured, target); state.hash1 ^= NS.zKey1(captured, target);
    }

    // lift moving piece off origin
    state.hash0 ^= NS.zKey0(piece, origin); state.hash1 ^= NS.zKey1(piece, origin);
    board[origin] = 0;

    // place piece (promoted type if applicable) on target
    var placed = (flag === NS.FLAG_PROMO) ? (promo | color) : piece;
    state.hash0 ^= NS.zKey0(placed, target); state.hash1 ^= NS.zKey1(placed, target);
    board[target] = placed;

    if (ptype === KING) { if (color === WHITE) state.whiteKing = target; else state.blackKing = target; }

    // new en-passant target (passed-over cell) only on a double pawn push
    var newEp = (flag === NS.FLAG_DOUBLE) ? ((origin + target) >> 1) : -1;
    if (newEp >= 0) { state.hash0 ^= NS.zEp0(newEp); state.hash1 ^= NS.zEp1(newEp); }
    state.ep = newEp;

    // 50-move clock: reset on pawn move or any capture
    if (ptype === PAWN || captured !== 0) state.halfmove = 0; else state.halfmove++;

    // flip side to move
    state.player ^= COLOR;
    state.hash0 ^= NS.zSide0; state.hash1 ^= NS.zSide1;
  };

  NS.undoMove = function (state) {
    var rec = state.history.pop();
    state.player ^= COLOR;                 // restore mover as side to move
    state.ep = rec.prevEp;
    state.halfmove = rec.prevHalfmove;
    state.whiteKing = rec.prevWK;
    state.blackKing = rec.prevBK;
    state.hash0 = rec.prevH0;
    state.hash1 = rec.prevH1;

    var board = state.board, move = rec.move;
    var origin = NS.moveOrigin(move), target = NS.moveTarget(move), flag = NS.moveFlag(move);
    var color = state.player & COLOR;

    if (flag === NS.FLAG_PROMO) {
      board[origin] = PAWN | color;        // promoted piece is discarded
      board[target] = rec.captured;
    } else if (flag === NS.FLAG_ENPASSANT) {
      board[origin] = board[target];
      board[target] = 0;
      board[rec.epCapCell] = rec.captured; // restore the captured enemy pawn
    } else {
      board[origin] = board[target];
      board[target] = rec.captured;        // 0 for a double push, captured tile otherwise
    }
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
