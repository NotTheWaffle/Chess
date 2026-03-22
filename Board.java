
import java.util.Arrays;


public class Board {
	private static final char[] PIECE_SYMBOLS = {' ', 'p', 'r', 'n', 'b', 'q', 'k', ' ', ' ', 'P', 'R', 'N', 'B', 'Q', 'K', ' '};
	
	private final byte[] board;
	public Board(){
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR ");
	}

	public Board(Board board){
		this.board = Arrays.copyOf(board.board, 64);
	}
	
	@SuppressWarnings("OverridableMethodCallInConstructor")
	public Board(String fenString){
		board = new byte[64];
		String[] ranks = fenString.split(" ")[0].split("/");
		for (int y = 0; y < 8; y++){
			String row = ranks[7-y];
			int x = 0;
			for (char letter : row.toCharArray()){
				switch (letter){
					case 'r' -> setTile(x++, y, Tile.BLACK_ROOK);
					case 'n' -> setTile(x++, y, Tile.BLACK_KNIGHT);
					case 'b' -> setTile(x++, y, Tile.BLACK_BISHOP);
					case 'k' -> setTile(x++, y, Tile.BLACK_KING);
					case 'q' -> setTile(x++, y, Tile.BLACK_QUEEN);
					case 'p' -> setTile(x++, y, Tile.BLACK_PAWN);
					
					case 'R' -> setTile(x++, y, Tile.WHITE_ROOK);
					case 'N' -> setTile(x++, y, Tile.WHITE_KNIGHT);
					case 'B' -> setTile(x++, y, Tile.WHITE_BISHOP);
					case 'K' -> setTile(x++, y, Tile.WHITE_KING);
					case 'Q' -> setTile(x++, y, Tile.WHITE_QUEEN);
					case 'P' -> setTile(x++, y, Tile.WHITE_PAWN);

					default -> x += (letter-'0');
				}
			}
		}
	}


	public byte getTile(int x, int y){
		return board[(y<<3)|x];
	}
	public byte getTile(int idx){
		return board[idx];
	}


	public void setTile(int x, int y, int tile){
		board[x+(y<<3)] = (byte) tile;
	}
	public void setTile(int idx, int tile){
		board[idx] = (byte) tile;
	}

	public void setTile(int x, int y, byte tile){
		board[x+(y<<3)] = tile;
	}
	public void setTile(int idx, byte tile){
		board[idx] = tile;
	}
	
	@Override
	public int hashCode(){
		int hashCode = 1;
		for (int i = 0; i < 64; i++){
			hashCode = hashCode * 31 + board[i];
		}
		return hashCode;
	}
	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof Board b){
			for (int i = 0; i < 64; i++){
				if (board[i] != b.board[i]){
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		for (int y = 7; y >= 0; y--){
			for (int x = 0; x < 8; x++){
				result.append(PIECE_SYMBOLS[getTile(x, y)]);
			}
			result.append('\n');
		}
		return result.toString();
	}
}
