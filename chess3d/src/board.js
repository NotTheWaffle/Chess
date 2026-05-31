// board.js — the 512-cell board is a plain Int8Array (index = x + 8y + 64z).
// Access is mostly direct (board[i]); these are factory/debug helpers.
(function (NS) {
  NS.newBoard = function () { return new Int8Array(NS.CELLS); };
  NS.cloneBoard = function (b) { return b.slice(); };
  NS.boardsEqual = function (a, b) {
    for (var i = 0; i < NS.CELLS; i++) if (a[i] !== b[i]) return false;
    return true;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
