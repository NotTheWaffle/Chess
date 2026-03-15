
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GameState{
	List<Long> hashes;
	Board board;
	byte player;
	int enpassantIndex;
	boolean whiteKingCastle;
	boolean whiteQueenCastle;
	boolean blackKingCastle;
	boolean blackQueenCastle;
	int whiteKingIndex;
	int blackKingIndex;

	private static final long[] ZOBRIST_HASHING = generateZobristHashingValues();
	private static long[] generateZobristHashingValues(){
		Random random = new SecureRandom();
		long[] result = new long[14*64];
		for (int i = 0; i < 14*64; i++){
			result[i] = random.nextLong();
		}
		for (int i = 0; i < result.length; i++){
			for (int j = i+1; j < result.length; j++){
				if (result[i] == result[j]){
					System.out.println(i+" equals "+j);
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
			hashCode ^= ZOBRIST_HASHING[lookupIndex];
		}
		hashCode ^= ZOBRIST_HASHING[enpassantIndex+12*64];
		if (player == Tile.WHITE){
			hashCode ^= ZOBRIST_HASHING[13*64+1];
		} else {
			hashCode ^= ZOBRIST_HASHING[13*64+0];
		}
		if (whiteKingCastle) hashCode ^= ZOBRIST_HASHING[13*64+2];
		if (whiteQueenCastle) hashCode ^= ZOBRIST_HASHING[13*64+3];
		if (blackKingCastle) hashCode ^= ZOBRIST_HASHING[13*64+4];
		if (blackQueenCastle) hashCode ^= ZOBRIST_HASHING[13*64+5];
		return hashCode;
	}

	public GameState(){
		board = new Board();
		player = Tile.WHITE;
		enpassantIndex = -1;
		whiteKingCastle = whiteQueenCastle = blackKingCastle = blackQueenCastle = true;
		for (int i = 0; i < 64; i++){
			if (board.getTile(i) == Tile.BLACK_KING){
				blackKingIndex = i;
			} else if (board.getTile(i) == Tile.WHITE_KING){
				whiteKingIndex = i;
			}
		}
		hashes = new ArrayList<>();
		hashes.add(generateZobristHash());
	}
	public GameState(GameState gameState){
		this.board = new Board(gameState.board);
		this.player = gameState.player;
		this.enpassantIndex = gameState.enpassantIndex;
		this.whiteKingCastle = gameState.whiteKingCastle;
		this.whiteQueenCastle = gameState.whiteQueenCastle;
		this.blackKingCastle = gameState.blackKingCastle;
		this.blackQueenCastle = gameState.blackQueenCastle;
		this.whiteKingIndex = gameState.whiteKingIndex;
		this.blackKingIndex = gameState.blackKingIndex;
		this.hashes = new ArrayList<>(); hashes.addAll(gameState.hashes);
	}
	public void makeMove(Move move){
		if (move.origin() == 0 || move.target() == 0) whiteQueenCastle = false;
		if (move.origin() == 7 || move.target() == 7) whiteKingCastle = false;
		if (move.origin() == 4 || move.target() == 4) whiteKingCastle = whiteQueenCastle = false;
		
		if (move.origin() == 56 || move.target() == 56) blackQueenCastle = false;
		if (move.origin() == 63 || move.target() == 63) blackKingCastle = false;
		if (move.origin() == 60 || move.target() == 60) blackKingCastle = blackQueenCastle = false;
		
		board.setTile(move.target(), board.getTile(move.origin()));
		if (move.flag() != Move.FLAGLESS){
			int flag = move.flag();
			if (flag == Move.PAWN_DOUBLE){
				int d = -8;
				if (Tile.color(board.getTile(move.origin())) == Tile.WHITE){
					d = 8;
				}
				enpassantIndex = move.target() - d;
			} else {
				enpassantIndex = -1;
				if (flag == Move.EN_PASSANT_CAPTURE){
					int d = -8;
					if (Tile.color(board.getTile(move.origin())) == Tile.WHITE){
						d = 8;
					}
					board.setTile(move.target() - d, Tile.BLANK);
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
					board.setTile(move.target(), Move.getPiece(flag)|Tile.color(board.getTile(move.origin())));
				}
			}
		} else {
			enpassantIndex = -1;
		}
		board.setTile(move.origin(), Tile.BLANK);
		player = (byte)(player ^ Tile.COLOR);
		
		if (move.origin() == whiteKingIndex) whiteKingIndex = move.target();
		if (move.origin() == blackKingIndex) blackKingIndex = move.target();
		hashes.add(generateZobristHash());
	}
	@Override
	public int hashCode(){
		return (int) generateZobristHash();
	}
	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof GameState g){
			return g.board.equals(board) && g.blackKingCastle == blackKingCastle && g.whiteKingCastle == whiteKingCastle && g.blackQueenCastle == blackQueenCastle && g.whiteQueenCastle == whiteQueenCastle && g.player == player && g.enpassantIndex == enpassantIndex;
		} else {
			return false;
		}
	}
}