// engine.js — the public facade the view depends on. It wraps the mutable gamestate,
// tracks repetition + last move + an undo stack at the GAME level (distinct from the
// search's make/undo on the same state), and exposes view-friendly shapes: [x,y,z]
// cells, 'white'/'black' sides, status strings, and opaque move tokens.
(function (NS) {
  function cellXYZ(i) { return [NS.xOf(i), NS.yOf(i), NS.zOf(i)]; }

  NS.createEngine = function () {
    var state, repMap, moveStack;

    function repKey() { return state.hash0 + ':' + state.hash1; }
    function repCount() { return repMap[repKey()] || 0; }

    function wrap(m) {
      var o = NS.moveOrigin(m), t = NS.moveTarget(m), f = NS.moveFlag(m);
      return {
        raw: m,
        fromIndex: o, toIndex: t,
        from: cellXYZ(o), to: cellXYZ(t),
        flag: f, promo: NS.movePromo(m),
        isCapture: state.board[t] !== 0 || f === NS.FLAG_ENPASSANT
      };
    }

    function newGame() {
      state = NS.initState(NS.buildStartBoard(), NS.WHITE);
      repMap = {}; repMap[repKey()] = 1;
      moveStack = [];
    }
    newGame();

    return {
      raw: function () { return state; },
      newGame: newGame,
      getBoard: function () { return state.board; },
      getActiveSide: function () { return state.player === NS.WHITE ? 'white' : 'black'; },
      getKingCell: function (side) {
        var i = side === 'white' ? state.whiteKing : state.blackKing;
        return i < 0 ? null : cellXYZ(i);
      },
      getLastMove: function () { return moveStack.length ? moveStack[moveStack.length - 1] : null; },

      // legal moves for the piece at [x,y,z] (only the side to move) — array of tokens
      legalMovesFrom: function (xyz) {
        var moves = NS.legalMovesFrom(state, NS.index(xyz[0], xyz[1], xyz[2]));
        return moves.map(wrap);
      },
      allLegalMoves: function () { return NS.generateLegalMoves(state).map(wrap); },

      makeMove: function (token) {
        NS.makeMove(state, token.raw);
        var k = repKey(); repMap[k] = (repMap[k] || 0) + 1;
        moveStack.push(token);
      },
      undo: function () {
        if (moveStack.length === 0) return false;
        var k = repKey(); if (repMap[k]) { repMap[k]--; if (repMap[k] === 0) delete repMap[k]; }
        NS.undoMove(state);
        moveStack.pop();
        return true;
      },
      canUndo: function () { return moveStack.length > 0; },

      isInCheck: function (side) { return NS.isInCheck(state, side === 'white' ? NS.WHITE : NS.BLACK); },
      getStatus: function () { return NS.status(state, repCount()); },

      // synchronous, main-thread search (agent.js); returns a token or null
      searchBestMove: function (opts) {
        if (!NS.searchBestMove) return null;
        var m = NS.searchBestMove(state, opts || {});
        return m ? wrap(m) : null;
      }
    };
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
