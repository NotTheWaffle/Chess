// setup.js — the starting army. Each side stands on its lower four floors (z=0..3),
// leaving z=4..7 as open airspace for vertical maneuvering. Per floor: a back rank
// of majors (White at y=0, Black at y=7) and a full pawn rank (White y=1, Black y=6).
// Exactly one King per side, on the ground floor. Unicorns are featured on the upper
// floors so they launch triagonally into the airspace.
//
// BACK[z][x] = piece type on White's back rank at file x, level z (x-symmetric).
// This table is the main "feel" tunable — edit it (or POPULATED_FLOORS) freely.
(function (NS) {
  var R = NS.ROOK, N = NS.KNIGHT, B = NS.BISHOP, Q = NS.QUEEN, K = NS.KING, U = NS.UNICORN;

  var BACK = [
    [R, N, B, Q, K, B, N, R], // z=0 ground — the only King
    [R, N, B, U, U, B, N, R], // z=1
    [R, U, B, Q, Q, B, U, R], // z=2
    [U, N, U, B, B, U, N, U]  // z=3 top of the army
  ];
  var POPULATED_FLOORS = BACK.length; // 4

  NS.buildStartBoard = function () {
    var board = NS.newBoard();
    for (var z = 0; z < POPULATED_FLOORS; z++) {
      for (var x = 0; x < NS.SIZE; x++) {
        // White: back rank y=0, pawns y=1
        board[NS.index(x, 0, z)] = BACK[z][x] | NS.WHITE;
        board[NS.index(x, 1, z)] = NS.PAWN | NS.WHITE;
        // Black: back rank y=7, pawns y=6 (same file pattern -> kings face along y)
        board[NS.index(x, 7, z)] = BACK[z][x] | NS.BLACK;
        board[NS.index(x, 6, z)] = NS.PAWN | NS.BLACK;
      }
    }
    return board;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
