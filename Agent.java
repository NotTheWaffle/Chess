
import java.util.ArrayList;
import java.util.List;

public class Agent {
	private TranspositionTable tTable;
	public final int DEPTH;
	public Agent(int depth){
		this.DEPTH = depth;
		tTable = new TranspositionTable();
	}
	public Move findBestMove(GameState initialGameState){
		List<Move> moves = new ArrayList<>();
		new MoveGenerator(initialGameState).addLegalMoveForColor(initialGameState.player, moves);
		int bestEvaluation = Integer.MIN_VALUE;
		if (moves.isEmpty()){
			return new Move(0, 0);
		}
		Move bestMove = new Move(0, 0);
		List<Move> sorted = new ArrayList<>();
		for (Move move : moves){
			if (Tile.piece(initialGameState.board.getTile(move.target())) != Tile.BLANK){
				sorted.addFirst(move);
			} else {
				sorted.addLast(move);
			}
		}
		System.out.println("Possible Moves:"+sorted.size());
		for (Move move : sorted){
			GameState gameState = MoveGenerator.makeMove(move, initialGameState);
			System.out.print("After move: "+move);
			int evaluation = -evaluatePosition(gameState, DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (evaluation > bestEvaluation){
				bestEvaluation = evaluation;
				bestMove = move;
			}
		}
		tTable.updatedata(initialGameState, bestEvaluation, DEPTH, bestMove);
		System.out.println("Making move: "+bestMove+" out of "+moves.size()+" with an evaluation of "+bestEvaluation);
		System.out.println("Transposition table size: "+tTable.size());
		return bestMove;
	}
	public int evaluatePosition(GameState gameState, int depth, int alpha, int beta){
		if (depth == 0) return relativeEvaluation(gameState.board, gameState.player);
		if (tTable.contains(gameState, depth)){
			return tTable.getData(gameState).evaluation;
		}
		long gameHash = gameState.generateZobristHash();
		int i = 0;
		for (long hash : gameState.hashes){
			if (gameHash == hash){
				i++;
			}
		}
		if (i >= 3){
			System.out.println("draw ");
			//return 0;
		}
		List<Move> moves = new ArrayList<>();
		MoveGenerator gen = new MoveGenerator(gameState);
		gen.addLegalMoveForColor(gameState.player, moves);
		if (DEPTH-depth < 1){
			System.out.print(" at depth: "+(DEPTH-depth)+" there are "+moves.size()+" moves, with the best evaluation at: ");
		}
		int bestEvaluation = Integer.MIN_VALUE;
		if (moves.isEmpty()){
			if (gen.gameState.player == Tile.WHITE){
				if (!gen.isAttacked(gen.gameState.whiteKingIndex, Tile.BLACK)){
					bestEvaluation = 0;
				}
			} else {
				if (!gen.isAttacked(gen.gameState.blackKingIndex, Tile.WHITE)){
					bestEvaluation = 0;
				}
			}
			tTable.updatedata(gameState, bestEvaluation, depth, null);
			return bestEvaluation;
		}
		List<Move> sorted = new ArrayList<>();
		for (Move move : moves){
			if (Tile.piece(gameState.board.getTile(move.target())) != Tile.BLANK){
				sorted.addFirst(move);
			} else {
				sorted.addLast(move);
			}
		}
		for (Move move : sorted){
			GameState nextGameState = MoveGenerator.makeMove(move, gameState);
			int evaluation = -evaluatePosition(nextGameState, depth-1, -beta, -alpha);
			if (evaluation > bestEvaluation){
				bestEvaluation = evaluation;
			}
			if (bestEvaluation > alpha){
				alpha = bestEvaluation;
			}
			if (alpha >= beta){
				break;
			}
		}
		tTable.updatedata(gameState, bestEvaluation, depth, null);
		if (DEPTH-depth < 1) System.out.println(bestEvaluation);
		return bestEvaluation;
	}
	
	public static int materialEvaluation(Board board, byte color){
		int[] values = {0, 100, 500, 320, 330, 900, 20_000, 0};
		int result = 0;
		for (int i = 0; i < 64; i++){
			byte piece = board.getTile(i);
			if (Tile.color(piece) == color){
				result += values[piece&Tile.PIECE];
			}
		}
		return result;
	}
	public static int evaluation(Board board, byte color){
		return materialEvaluation(board, color)+positionEvaluation(board, color);
	}
	private static final int[] PAWN = {0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] ROOK = {0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0};
	private static final int[] KNIGHT = {-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50};
	private static final int[] BISHOP = {-20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10, -10, 5, 0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20};
	private static final int[] QUEEN = {-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20};
	private static final int[] KING_EARLY = {-30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20};
	private static final int[] KING_LATE = {-50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50};
	private static final int[][] LOOKUP = {new int[0], PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING_EARLY, KING_LATE};
	public static int positionEvaluation(Board board, byte color){
		int result = 0;
		for (int i = 0; i < 64; i++){
			byte piece = board.getTile(i);
			if (Tile.color(piece) == color){
				if (Tile.piece(piece) != Tile.BLANK){
					if (Tile.piece(piece) == Tile.KING) {
						// maybe endgame, maybe not
					}
					int j = i;
					if (color == Tile.WHITE){
						j = j&0b111+56-8*(j>>>3);
					}
					result += LOOKUP[Tile.piece(piece)][j];
				}
			}
		}
		return result;
	}
	public static int relativeEvaluation(Board board, byte color){
		return evaluation(board, color)-evaluation(board, (byte)(color^Tile.COLOR));
	}
}
