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
			default -> Tile.BLANK;
		};
	}
	public static String flagMeaning(int flag){
		return switch (flag){
			case FLAGLESS -> "";
			case EN_PASSANT_CAPTURE -> "via en passant";
			case PAWN_DOUBLE -> "via double pawn";
			case KNIGHT_PROMOTE -> "promoting to a Knight";
			case BISHOP_PROMOTE -> "promoting to a Bishop";
			case ROOK_PROMOTE -> "promoting to a Rook";
			case QUEEN_PROMOTE -> "promoting to a Queen";
			case WHITE_KING_CASTLING -> "via castling";
			case WHITE_QUEEN_CASTLING -> "via castling";
			case BLACK_KING_CASTLING -> "via castling";
			case BLACK_QUEEN_CASTLING -> "via castling";
			default -> "";
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
	public int getOriginIndex(){
		return data&0b111111;
	}
	public int getTargetIndex(){
		return (data>>>6)&0b111111;
	}
	public int getFlag(){
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
		if (o == this) return true;
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
		return getOriginIndex()+" to "+getTargetIndex()+"("+flagMeaning(getFlag())+")";
	}
	public String toPGNString(Board board){
		int flag = getFlag();
		// castling
		if (flag == WHITE_KING_CASTLING || flag == BLACK_KING_CASTLING) return "O-O";
		if (flag == WHITE_QUEEN_CASTLING || flag == BLACK_QUEEN_CASTLING) return "O-O-O";

		int rank = getTargetY();
		int file = getTargetX();
		byte originTile = board.getTile(getOriginIndex());
		int piece = originTile & Tile.PIECE;
		boolean isCapture = Tile.piece(board.getTile(getTargetIndex())) != Tile.BLANK
			|| flag == EN_PASSANT_CAPTURE;

		StringBuilder sb = new StringBuilder();
		if (piece == Tile.PAWN) {
			if (isCapture) sb.append("abcdefgh".charAt(getOriginX()));
		} else {
			sb.append(switch (piece){
				case Tile.ROOK -> "R";
				case Tile.KNIGHT -> "N";
				case Tile.BISHOP -> "B";
				case Tile.QUEEN -> "Q";
				case Tile.KING -> "K";
				default -> "";
			});
		}
		if (isCapture) sb.append('x');
		sb.append("abcdefgh".charAt(file));
		sb.append(rank + 1);
		// promotion
		if ((flag & PROMOTION) > 0) {
			sb.append('=');
			sb.append(switch (flag){
				case KNIGHT_PROMOTE -> "N";
				case BISHOP_PROMOTE -> "B";
				case ROOK_PROMOTE -> "R";
				case QUEEN_PROMOTE -> "Q";
				default -> "";
			});
		}
		return sb.toString();
	}
	public String toPGNString(GameState gameState){
		String base = toPGNString(gameState.board);
		// check/checkmate detection
		GameState copy = new GameState(gameState);
		copy.makeMove(this);
		MoveHandler handler = new MoveHandler(copy);
		java.util.List<Move> legalMoves = new java.util.ArrayList<>();
		handler.addLegalMoves(legalMoves);
		int kingPos = (copy.player == Tile.WHITE) ? copy.whiteKingIndex : copy.blackKingIndex;
		boolean inCheck = handler.isAttacked(kingPos, (byte)(copy.player ^ Tile.COLOR));
		if (inCheck) {
			if (legalMoves.isEmpty()) return base + "#";
			return base + "+";
		}
		return base;
	}
}
