// agent.js — the AI. Negamax + alpha-beta + iterative deepening, with capture-first
// move ordering (MVV-LVA). Ported in spirit from the Java Agent. The cube's branching
// factor (hundreds of moves/position) caps practical depth at ~2-4 ply on the main
// thread — this is a competent move-finder, not a strong player (by design).
//
// Mate scores are FINITE and ply-adjusted (-MATE + ply), never ±Infinity, so negamax
// negation stays well-defined (the -Integer.MIN_VALUE trap from the Java repo).
(function (NS) {
  var MATE = NS.MATE, INF = NS.INF;
  var nodes = 0;

  // -------------------------------------------------------------------------
  // Evaluation — score from the side-to-move's perspective. This is the AI's
  // "personality": material dominates, with a small bonus for centralizing
  // non-pawn pieces (more lines = more mobility in 3D). Tunable: PIECE_VALUES
  // live in constants.js; CENTER_WEIGHT scales the positional nudge.
  // -------------------------------------------------------------------------
  var CENTER_WEIGHT = 2;
  function evaluate(state) {
    var board = state.board, score = 0, i, t, type, x, y, z, d, pieceScore;
    for (i = 0; i < NS.CELLS; i++) {
      t = board[i]; if (t === 0) continue;
      type = t & NS.PIECE_MASK;
      pieceScore = NS.PIECE_VALUES[type];
      if (type !== NS.PAWN && type !== NS.KING) {
        x = NS.xOf(i); y = NS.yOf(i); z = NS.zOf(i);
        d = Math.abs(x - 3.5) + Math.abs(y - 3.5) + Math.abs(z - 3.5); // 1.5 (center) .. 10.5 (corner)
        pieceScore += (10.5 - d) * CENTER_WEIGHT;
      }
      if ((t & NS.COLOR_BIT) === NS.WHITE) score += pieceScore; else score -= pieceScore;
    }
    return (state.player === NS.WHITE) ? score : -score;
  }

  function victimValue(state, move) {
    if (NS.moveFlag(move) === NS.FLAG_ENPASSANT) return NS.PIECE_VALUES[NS.PAWN];
    var t = state.board[NS.moveTarget(move)];
    return t ? NS.PIECE_VALUES[t & NS.PIECE_MASK] : 0;
  }

  // captures first, most-valuable-victim / least-valuable-attacker
  function orderMoves(state, moves) {
    var scored = moves.map(function (m) {
      var v = victimValue(state, m);
      var a = NS.PIECE_VALUES[state.board[NS.moveOrigin(m)] & NS.PIECE_MASK];
      return { m: m, s: v > 0 ? (1000000 + v * 16 - a) : 0 };
    });
    scored.sort(function (p, q) { return q.s - p.s; });
    return scored.map(function (o) { return o.m; });
  }

  function negamax(state, depth, alpha, beta, ply) {
    nodes++;
    if (depth <= 0) return evaluate(state);
    var moves = NS.generateLegalMoves(state);
    if (moves.length === 0) return NS.isInCheck(state, state.player) ? (-MATE + ply) : 0;
    moves = orderMoves(state, moves);
    var best = -INF;
    for (var i = 0; i < moves.length; i++) {
      NS.makeMove(state, moves[i]);
      var score = -negamax(state, depth - 1, -beta, -alpha, ply + 1);
      NS.undoMove(state);
      if (score > best) best = score;
      if (best > alpha) alpha = best;
      if (alpha >= beta) break; // beta cutoff
    }
    return best;
  }

  NS.searchBestMove = function (state, opts) {
    opts = opts || {};
    var maxDepth = opts.depth || 2;
    var timeMs = opts.timeMs || 8000;
    var clock = function () { return (typeof performance !== 'undefined') ? performance.now() : Date.now(); };
    var deadline = clock() + timeMs;

    var rootMoves = NS.generateLegalMoves(state);
    if (rootMoves.length === 0) return null;
    rootMoves = orderMoves(state, rootMoves);
    var bestMove = rootMoves[0];
    nodes = 0;

    for (var d = 1; d <= maxDepth; d++) {
      var alpha = -INF, localBest = -INF, localBestMove = rootMoves[0];
      for (var i = 0; i < rootMoves.length; i++) {
        NS.makeMove(state, rootMoves[i]);
        var score = -negamax(state, d - 1, -INF, -alpha, 1);
        NS.undoMove(state);
        if (score > localBest) { localBest = score; localBestMove = rootMoves[i]; }
        if (localBest > alpha) alpha = localBest;
      }
      bestMove = localBestMove;
      // put the best move first so the next, deeper iteration prunes harder
      var bi = rootMoves.indexOf(bestMove);
      if (bi > 0) { rootMoves.splice(bi, 1); rootMoves.unshift(bestMove); }
      if (Math.abs(localBest) > MATE - 1000) break; // forced mate found
      if (clock() > deadline) break;                // out of time
    }
    return bestMove;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
