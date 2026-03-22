
import java.security.SecureRandom;
import java.util.Random;


public class GameState{
	public Board board;
	public byte player;
	public byte enpassantIndex;
	public byte castlingRights;
	// not neccessary, just for faster lookups
	public transient byte whiteKingIndex;
	public transient byte blackKingIndex;

	private static final long[] ZOBRIST_HASHING_RANDOMS = generateZobristHashingRandoms();
	private static long[] generateZobristHashingRandoms(){
		Random random = new SecureRandom();
		long[] result = new long[14*64];
		for (int i = 0; i < 14*64; i++){
			result[i] = random.nextLong();
		}
		for (int i = 0; i < result.length; i++){
			for (int j = i+1; j < result.length; j++){
				if (result[i] == result[j]){
					System.out.println(i+" equals "+j+" (very bad)");
				}
			}
		}
		return result;
	}
	public long generateZobristHash(){
		long hashCode = 0;
		for (int i = 0; i < 64; i++){
			int lookupIndex = 0;
			byte tile = board.getTile(i);
			if (Tile.piece(tile) != Tile.BLANK){
				lookupIndex = ((tile&Tile.PIECE)-1)+6*((tile&Tile.COLOR)>>>3);
				lookupIndex += i * 12;
			}
			hashCode ^= ZOBRIST_HASHING_RANDOMS[lookupIndex];
		}
		hashCode ^= ZOBRIST_HASHING_RANDOMS[enpassantIndex+12*64];
		if (player == Tile.WHITE){
			hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+1];
		} else {
			hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+0];
		}
		if ((castlingRights & 0b0001) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+2];
		if ((castlingRights & 0b0010) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+3];
		if ((castlingRights & 0b0100) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+4];
		if ((castlingRights & 0b1000) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+5];
		return hashCode;
	}

	public GameState(){
		board = new Board();
		player = Tile.WHITE;
		enpassantIndex = -1;
		castlingRights = 0b1111;
		for (byte i = 0; i < 64; i++){
			if (board.getTile(i) == Tile.BLACK_KING){
				blackKingIndex = i;
			} else if (board.getTile(i) == Tile.WHITE_KING){
				whiteKingIndex = i;
			}
		}
	}
	public GameState(String fenString){
		board = new Board(fenString);

		String playerToMove = fenString.split(" ")[1];
		player = playerToMove.equals("w") ? Tile.WHITE : Tile.BLACK;
		
		String castling = fenString.split(" ")[2];
		castlingRights = 0b0000;
		// qkQK
		if (castling.contains("K")){
			castlingRights |= 0b0001;
		}
		if (castling.contains("Q")){
			castlingRights |= 0b0010;
		}
		if (castling.contains("k")){
			castlingRights |= 0b0100;
		}
		if (castling.contains("q")){
			castlingRights |= 0b1000;
		}

		String enpassant = fenString.split(" ")[3];
		if (enpassant.equals("-")){
			enpassantIndex = -1;
		} else {
			enpassantIndex = (byte) (enpassant.charAt(0)-'a'+8*(enpassant.charAt(1)-'1'));
		}
		
		for (byte i = 0; i < 64; i++){
			if (board.getTile(i) == Tile.BLACK_KING){
				blackKingIndex = i;
			} else if (board.getTile(i) == Tile.WHITE_KING){
				whiteKingIndex = i;
			}
		}
	}
	public GameState(GameState gameState){
		this.board = new Board(gameState.board);
		this.player = gameState.player;
		this.enpassantIndex = gameState.enpassantIndex;
		this.castlingRights = gameState.castlingRights;
		this.whiteKingIndex = gameState.whiteKingIndex;
		this.blackKingIndex = gameState.blackKingIndex;
	}
	public void makeMove(Move move){
		if (move.getOriginIndex() == 0 || move.getTargetIndex() == 0) castlingRights &= 0b1101;	//white queen
		if (move.getOriginIndex() == 7 || move.getTargetIndex() == 7) castlingRights &= 0b1110;	//white king
		if (move.getOriginIndex() == 4 || move.getTargetIndex() == 4) castlingRights &= 0b1100;
		
		if (move.getOriginIndex() == 56 || move.getTargetIndex() == 56) castlingRights &= 0b0111;	//black queen
		if (move.getOriginIndex() == 63 || move.getTargetIndex() == 63) castlingRights &= 0b1011;	//black king
		if (move.getOriginIndex() == 60 || move.getTargetIndex() == 60) castlingRights &= 0b0011;
		
		board.setTile(move.getTargetIndex(), board.getTile(move.getOriginIndex()));
		if (move.getFlag() != Move.FLAGLESS){
			int flag = move.getFlag();
			if (flag == Move.PAWN_DOUBLE){
				int d = -8;
				if (Tile.color(board.getTile(move.getOriginIndex())) == Tile.WHITE){
					d = 8;
				}
				enpassantIndex = (byte) (move.getTargetIndex() - d);
			} else {
				enpassantIndex = -1;
				if (flag == Move.EN_PASSANT_CAPTURE){
					int d = -8;
					if (Tile.color(board.getTile(move.getOriginIndex())) == Tile.WHITE){
						d = 8;
					}
					board.setTile(move.getTargetIndex() - d, Tile.BLANK);
				} else if ((flag & Move.CASTLING) > 0){
					switch (flag){
						case Move.BLACK_KING_CASTLING -> {
							board.setTile(61, Tile.BLACK_ROOK);
							board.setTile(63, Tile.BLANK);
						}
						case Move.BLACK_QUEEN_CASTLING -> {
							board.setTile(59, Tile.BLACK_ROOK);
							board.setTile(56, Tile.BLANK);
						}
						case Move.WHITE_KING_CASTLING -> {
							board.setTile(5, Tile.WHITE_ROOK);
							board.setTile(7, Tile.BLANK);
						}
						case Move.WHITE_QUEEN_CASTLING -> {
							board.setTile(3, Tile.WHITE_ROOK);
							board.setTile(0, Tile.BLANK);
						}
					}
				} else if ((flag & Move.PROMOTION) > 0){
					board.setTile(move.getTargetIndex(), Move.getPiece(flag)|Tile.color(board.getTile(move.getOriginIndex())));
				}
			}
		} else {
			enpassantIndex = -1;
		}
		board.setTile(move.getOriginIndex(), Tile.BLANK);
		player = (byte)(player ^ Tile.COLOR);
		
		if (move.getOriginIndex() == whiteKingIndex) whiteKingIndex = (byte) move.getTargetIndex();
		if (move.getOriginIndex() == blackKingIndex) blackKingIndex = (byte) move.getTargetIndex();
		
	}
	@Override
	public int hashCode(){
		return (int) generateZobristHash();
	}
	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof GameState g){
			return g.board.equals(board) && g.castlingRights == castlingRights && g.player == player && g.enpassantIndex == enpassantIndex;
		} else {
			return false;
		}
	}
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		String p = player == Tile.WHITE ? "White" : "Black";
		result.append("Turn:"+p);
		result.append("\nEn passant:"+enpassantIndex);
		result.append("\nCastling:"+Integer.toBinaryString(castlingRights));
		result.append("\n"+board.toString());
		return result.toString();
	}
}