public class Main{
	public static void main(String[] args){
		ChessGame chessGame = new ChessGame(Tile.WHITE, false);
		Window window = new Window(chessGame);
	}
}