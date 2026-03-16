public class Main{
	public static void main(String[] args){
		ChessGame chessGame = new ChessGame(Tile.BLACK, true);
		Window window = new Window(chessGame);
	}
}