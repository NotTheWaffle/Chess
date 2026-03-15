public class Move {
	public final int data;
	public final static int FLAGLESS           = 0b0000;
	public final static int EN_PASSANT_CAPTURE = 0b0001;
	public final static int PAWN_DOUBLE        = 0b0010;
	
	public final static int PROMOTION      = 0b0100;
	public final static int KNIGHT_PROMOTE = 0b0100;
	public final static int BISHOP_PROMOTE = 0b0101;
	public final static int ROOK_PROMOTE   = 0b0110;
	public final static int QUEEN_PROMOTE  = 0b0111;
	
	public final static int CASTLING             = 0b1000;
	public final static int WHITE_KING_CASTLING  = 0b1000;
	public final static int WHITE_QUEEN_CASTLING = 0b1001;
	public final static int BLACK_KING_CASTLING  = 0b1010;
	public final static int BLACK_QUEEN_CASTLING = 0b1011;

	public static int getPiece(int flag){
		return switch (flag){
			case KNIGHT_PROMOTE -> Tile.KNIGHT;
			case BISHOP_PROMOTE -> Tile.BISHOP;
			case ROOK_PROMOTE -> Tile.ROOK;
			case QUEEN_PROMOTE -> Tile.QUEEN;
			default -> 0;
		};
	}
	public Move(int data){
		this.data = data;
	}
	public Move(int origin, int destination){
		this(origin, destination, FLAGLESS);
	}
	public Move(int origin, int target, int flag){
		this.data = flag<<12|target<<6|origin;
	}
	public Move(int x1, int y1, int x2, int y2){
		this(x1, y1, x2, y2, FLAGLESS);
	}
	public Move(int x1, int y1, int x2, int y2, int flag){
		this(x1 + y1*8, x2 + y2*8, flag);
	}
	public int origin(){
		return data&0b111111;
	}
	public int target(){
		return (data>>>6)&0b111111;
	}
	public int flag(){
		return data>>>12;
	}
	public int getOriginX(){
		return data&0b111;
	}
	public int getOriginY(){
		return (data>>>3)&0b111;
	}
	public int getTargetX(){
		return (data>>>6)&0b111;
	}
	public int getTargetY(){
		return (data>>>9)&0b111;
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof Move m){
			return m.data == data;
		} else {
			return false;
		}
	}
	@Override
	public int hashCode(){
		return data;
	}
	@Override
	public String toString(){
		return origin()+" to "+target()+"("+flag()+")";
	}
}
