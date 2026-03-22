
import java.util.ArrayList;
import java.util.List;

public class Main{
	public static void main(String[] args){
		GameState gameState = new GameState("r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1 ");
		System.out.println(gameState);
			GameState copy = new GameState(gameState);
		List<Move> moves = new ArrayList<>();
		MoveHandler moveHandler = new MoveHandler(gameState);
		moveHandler.addLegalMoves(moves);
		for (Move move : moves){
			moveHandler.tryMove(move);
			moveHandler.untryMove();
			if (!gameState.equals(copy)){
				System.out.println("failed");
			} else {
			}
		}

		ChessGame chessGame = new ChessGame(false, true, true);
		Window window = new Window(chessGame);
	}
}