// constants.js — piece/color encoding, move flags, piece values. Ported and widened
// from the Java Tile.java / Move.java. A tile is one byte:  piece type in bits 0-3,
// color in bit 4. The 4-bit piece field (vs Java's 3) reserves room for UNICORN(7)
// and a future TITAN(8) without ever clashing with the color bit.
(function (NS) {
  // --- piece types (low 4 bits) ---
  NS.EMPTY   = 0;
  NS.PAWN    = 1;
  NS.ROOK    = 2;
  NS.KNIGHT  = 3;
  NS.BISHOP  = 4;
  NS.QUEEN   = 5;
  NS.KING    = 6;
  NS.UNICORN = 7;   // NEW — the triagonal (space-diagonal) slider
  // NS.TITAN = 8;  // reserved (all-26 slider) — not shipped

  NS.PIECE_MASK = 0x0F;   // bits 0-3
  NS.COLOR_BIT  = 0x10;   // bit 4
  NS.WHITE = 0x10;        // color bit set
  NS.BLACK = 0x00;        // color bit clear

  // accessors (always guard tile !== EMPTY before reading color)
  NS.typeOf  = function (tile) { return tile & NS.PIECE_MASK; };
  NS.colorOf = function (tile) { return tile & NS.COLOR_BIT; };
  NS.isWhite = function (tile) { return (tile & NS.COLOR_BIT) !== 0; };
  NS.opp     = function (color) { return color ^ NS.COLOR_BIT; };

  // --- move flags (3-bit field) ---
  NS.FLAG_NORMAL    = 0;
  NS.FLAG_DOUBLE    = 1;  // pawn two-square advance (sets en-passant target)
  NS.FLAG_ENPASSANT = 2;  // pawn en-passant capture
  NS.FLAG_PROMO     = 3;  // pawn promotion (promo piece in separate field)

  // --- piece values, indexed by piece type (king = 0, mate handles it) ---
  NS.PIECE_VALUES = [0, 100, 500, 320, 330, 900, 0, 310];
  //                 e  P    R    N    B    Q    K  U

  // single-letter labels for UI / notation, indexed by piece type
  NS.PIECE_LETTERS = ['', 'P', 'R', 'N', 'B', 'Q', 'K', 'U'];

  // promotion choices offered (Queen first = default)
  NS.PROMO_OPTIONS = [NS.QUEEN, NS.ROOK, NS.BISHOP, NS.KNIGHT, NS.UNICORN];

  // forward step in flat-index space for a pawn of the given color (+y / -y => ±8)
  NS.forwardStep = function (color) { return color === NS.WHITE ? 8 : -8; };

  // mate/score sentinels (FINITE — never ±Infinity; see commit 06ab434 note)
  NS.MATE = 1000000;
  NS.INF  = 1000000000;
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
