// move.js — moves packed into a 24-bit integer (fits a JS 32-bit bitwise int):
//   origin[9] | target[9] | flag[3] | promo[3]
// 9 bits per cell index (0..511). NOTE: do NOT reuse the Java 6-bit <<6 layout.
(function (NS) {
  var CELL_MASK = 0x1FF; // 9 bits
  var FLAG_MASK = 0x7;   // 3 bits
  var PROMO_MASK = 0x7;  // 3 bits

  NS.encodeMove = function (origin, target, flag, promo) {
    return (origin & CELL_MASK)
      | ((target & CELL_MASK) << 9)
      | ((flag & FLAG_MASK) << 18)
      | ((promo & PROMO_MASK) << 21);
  };
  NS.moveOrigin = function (m) { return m & CELL_MASK; };
  NS.moveTarget = function (m) { return (m >> 9) & CELL_MASK; };
  NS.moveFlag   = function (m) { return (m >> 18) & FLAG_MASK; };
  NS.movePromo  = function (m) { return (m >> 21) & PROMO_MASK; };

  // human-readable cell, e.g. (0,0,0) -> "a1L1"  (file a-h, rank 1-8, level L1-L8)
  NS.cellStr = function (i) {
    return String.fromCharCode(97 + NS.xOf(i)) + (NS.yOf(i) + 1) + 'L' + (NS.zOf(i) + 1);
  };

  NS.moveStr = function (m) {
    var s = NS.cellStr(NS.moveOrigin(m)) + '→' + NS.cellStr(NS.moveTarget(m));
    if (NS.moveFlag(m) === NS.FLAG_PROMO) s += '=' + NS.PIECE_LETTERS[NS.movePromo(m)];
    return s;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
