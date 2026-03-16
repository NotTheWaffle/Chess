public final class Tile {
	public static final byte PIECE  = 0b111;

	public static final byte BLANK  = 0b000; //0
	public static final byte PAWN   = 0b001; //1
	public static final byte ROOK   = 0b010; //2
	public static final byte KNIGHT = 0b011; //3
	public static final byte BISHOP = 0b100; //4
	public static final byte QUEEN  = 0b101; //5
	public static final byte KING   = 0b110; //6
	public static final byte UNUSED = 0b111; //7

	
	public static final byte COLOR  = 0b1000;

	public static final byte BLACK  = 0b0000; //0
	public static final byte WHITE  = 0b1000; //8

	
	public static final byte BLACK_PAWN   = BLACK|PAWN;   // 1
	public static final byte BLACK_ROOK   = BLACK|ROOK;   // 2
	public static final byte BLACK_KNIGHT = BLACK|KNIGHT; // 3
	public static final byte BLACK_BISHOP = BLACK|BISHOP; // 4
	public static final byte BLACK_QUEEN  = BLACK|QUEEN;  // 5
	public static final byte BLACK_KING   = BLACK|KING;   // 6
	
	public static final byte WHITE_PAWN   = WHITE|PAWN;   // 9
	public static final byte WHITE_ROOK   = WHITE|ROOK;   // 10
	public static final byte WHITE_KNIGHT = WHITE|KNIGHT; // 11
	public static final byte WHITE_BISHOP = WHITE|BISHOP; // 12
	public static final byte WHITE_QUEEN  = WHITE|QUEEN;  // 13
	public static final byte WHITE_KING   = WHITE|KING;   // 14

	private Tile(){}
	public static byte piece(byte tile){
		return (byte) (tile & PIECE);
	}
	public static byte color(byte tile){
		return (byte) (tile & COLOR);
	}
}
