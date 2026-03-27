
import java.util.ArrayList;
import java.util.List;

public class Agent {
	private final TranspositionTable tTable;
	public final int depth;
	private final static int CHECKMATE = -1_000_000;
	private final static int STALEMATE = 0;
	public Agent(int depth){
		this.depth = depth;
		tTable = new TranspositionTable();
	}
	public Move findBestMove(GameState gameState){
		List<Move> moves = new ArrayList<>();
		MoveHandler moveHandler = new MoveHandler(gameState);
		moveHandler.addLegalMoves(moves);

		int bestEvaluation = -1_000_000_000;
		if (moves.isEmpty()){
			System.out.println("No available moves");
			return null;
		}
		Move bestMove = new Move(0, 0);
		List<Move> sorted = new ArrayList<>();
		for (Move move : moves){
			if (Tile.piece(gameState.board.getTile(move.getTargetIndex())) != Tile.BLANK){
				sorted.addFirst(move);
			} else {
				sorted.addLast(move);
			}
		}
		for (Move move : sorted){
			GameState newGamestate = moveHandler.makeMoveClone(move);
			//moveHandler.tryMove(move);
			int evaluation = -evaluatePosition(newGamestate, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
			//moveHandler.untryMove();
			if (evaluation > bestEvaluation){
				bestEvaluation = evaluation;
				bestMove = move;
			}
			System.out.println(move.toPGNString(gameState.board)+":"+evaluation);
		}
		tTable.updatedata(gameState, bestEvaluation, depth, bestMove);
		System.out.println("Current Evaluation:"+evaluatePosition(gameState, depth, Integer.MIN_VALUE, Integer.MAX_VALUE));
		System.out.println("Choice: ("+bestMove.toPGNString(gameState.board)+":"+bestEvaluation+")");
		return bestMove;
	}
	public int evaluatePosition(MoveHandler moveHandler, int depth, int alpha, int beta){
		if (depth == 0) return relativeEvaluation(moveHandler.gameState.board, moveHandler.gameState.player);
		
		List<Move> moves = new ArrayList<>();
		moveHandler.addLegalMoves(moves);
		int bestEvaluation = -1_000_000_000;

		// return checkmate and stalemate conditions
		if (moves.isEmpty()){
			if (moveHandler.gameState.player == Tile.WHITE){
				if (!moveHandler.isAttacked(moveHandler.gameState.whiteKingIndex, Tile.BLACK)){
					bestEvaluation = STALEMATE;
				} else {
					bestEvaluation = CHECKMATE+this.depth-depth;
				}
			} else {
				if (!moveHandler.isAttacked(moveHandler.gameState.blackKingIndex, Tile.WHITE)){
					bestEvaluation = STALEMATE;
				} else {
					bestEvaluation = CHECKMATE+this.depth-depth;
				}
			}
			return bestEvaluation;
		}
		List<Move> sorted = new ArrayList<>();
		// place the captures at the start of the list
		for (Move move : moves){
			if (Tile.piece(moveHandler.gameState.board.getTile(move.getTargetIndex())) != Tile.BLANK){
				sorted.addFirst(move);
			} else {
				sorted.addLast(move);
			}
		}
		for (Move move : sorted){
			moveHandler.tryMove(move);
			int evaluation = -evaluatePosition(moveHandler, depth-1, -beta, -alpha);
			moveHandler.untryMove();
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
		return bestEvaluation;
	}
	public int evaluatePosition(GameState gameState, int depth, int alpha, int beta){
		if (depth == 0) return relativeEvaluation(gameState.board, gameState.player);
		if (tTable.contains(gameState, depth)){
			return tTable.getData(gameState).evaluation;
		}
		List<Move> moves = new ArrayList<>();
		MoveHandler gen = new MoveHandler(gameState);
		gen.addLegalMovesForColor(gameState.player, moves);
		int bestEvaluation = -1_000_000_000;

		// return -1_000_000 or 0 if its checkmate or stalemate
		if (moves.isEmpty()){
			if (gen.gameState.player == Tile.WHITE){
				if (!gen.isAttacked(gen.gameState.whiteKingIndex, Tile.BLACK)){
					bestEvaluation = STALEMATE;
				} else {
					bestEvaluation = CHECKMATE+this.depth-depth;
				}
			} else {
				if (!gen.isAttacked(gen.gameState.blackKingIndex, Tile.WHITE)){
					bestEvaluation = STALEMATE;
				} else {
					bestEvaluation = CHECKMATE+this.depth-depth;
				}
			}
			tTable.updatedata(gameState, bestEvaluation, depth, null);
			return bestEvaluation;
		}
		List<Move> sorted = new ArrayList<>();
		// place the captures at the start of the list
		for (Move move : moves){
			if (Tile.piece(gameState.board.getTile(move.getTargetIndex())) != Tile.BLANK){
				sorted.addFirst(move);
			} else {
				sorted.addLast(move);
			}
		}
		for (Move move : sorted){
			GameState nextGameState = gen.makeMoveClone(move);
		//	gen.tryMove(move);
			int evaluation = -evaluatePosition(nextGameState, depth-1, -beta, -alpha);
		//	gen.untryMove();
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
		return bestEvaluation;
	}
	
	public static int materialEvaluation(Board board, byte color){
		int[] values = {0, 100, 500, 320, 330, 900, 0, 0};
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
		int evaluation = materialEvaluation(board, color)+positionEvaluation(board, color);
		return evaluation;
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
