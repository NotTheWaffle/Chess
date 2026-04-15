
import java.util.ArrayList;
import java.util.List;

public class Agent {
	private final TranspositionTable tTable;
	public final int maxDepth;
	private long timeLimitMs;
	private long searchStartTime;
	private volatile boolean searchAborted;

	private final static int CHECKMATE = -1_000_000;
	private final static int STALEMATE = 0;
	private final static int INF = 1_000_000_000;
	private final static int MAX_Q_DEPTH = 8;
	private static final int[] PIECE_VALUES = {0, 100, 500, 320, 330, 900, 0, 0};

	// killer moves: 2 per ply
	private final int[][] killerMoves = new int[64][2];
	// history heuristic: [pieceType][targetSquare]
	private final int[][] historyTable = new int[7][64];

	// search statistics
	private long nodesEvaluated;
	private long quiescenceNodes;
	private long ttHits;

	public Agent(int depth){
		this.maxDepth = depth;
		this.timeLimitMs = 5000;
		tTable = new TranspositionTable();
	}
	public Agent(int depth, long timeLimitMs){
		this.maxDepth = depth;
		this.timeLimitMs = timeLimitMs;
		tTable = new TranspositionTable();
	}

	public Move findBestMove(GameState gameState){
		return findBestMove(gameState, timeLimitMs);
	}

	public Move findBestMove(GameState gameState, long timeLimitMs){
		MoveHandler moveHandler = new MoveHandler(gameState);
		List<Move> moves = new ArrayList<>();
		moveHandler.addLegalMoves(moves);

		if (moves.isEmpty()){
			System.out.println("No available moves");
			return null;
		}
		if (moves.size() == 1) return moves.getFirst();

		searchStartTime = System.currentTimeMillis();
		searchAborted = false;
		this.timeLimitMs = timeLimitMs;

		// clear killer and history tables
		for (int[] k : killerMoves) { k[0] = 0; k[1] = 0; }
		for (int[] h : historyTable) java.util.Arrays.fill(h, 0);

		Move bestMove = moves.getFirst();
		int bestEval = -INF;

		// iterative deepening
		for (int depth = 1; depth <= maxDepth; depth++){
			nodesEvaluated = 0;
			quiescenceNodes = 0;
			ttHits = 0;
			long iterStart = System.currentTimeMillis();

			int alpha = -INF;
			int beta = INF;
			int iterBestEval = -INF;
			Move iterBestMove = bestMove;

			// sort: put previous best move first
			final Move prevBest = bestMove;
			sortMoves(moves, gameState.board, null, 0);
			// ensure previous best is searched first
			for (int i = 0; i < moves.size(); i++) {
				if (moves.get(i).data == prevBest.data && i > 0) {
					moves.remove(i);
					moves.addFirst(prevBest);
					break;
				}
			}

			boolean aborted = false;
			for (Move move : moves){
				moveHandler.tryMove(move);
				int evaluation = -evaluatePosition(moveHandler, depth - 1, -beta, -alpha, true);
				moveHandler.untryMove();

				if (searchAborted) { aborted = true; break; }

				if (evaluation > iterBestEval){
					iterBestEval = evaluation;
					iterBestMove = move;
				}
				if (iterBestEval > alpha) alpha = iterBestEval;
			}

			if (!aborted) {
				bestMove = iterBestMove;
				bestEval = iterBestEval;
				long elapsed = System.currentTimeMillis() - iterStart;
				System.out.printf("depth %d: %s (%d) nodes=%d qnodes=%d tt=%d time=%dms%n",
					depth, bestMove.toPGNString(gameState.board), bestEval,
					nodesEvaluated, quiescenceNodes, ttHits, elapsed);

				// store in TT
				long hash = gameState.generateZobristHash();
				tTable.store(hash, bestEval, depth, bestMove, TranspositionTable.EXACT);

				// stop if we found a mate
				if (bestEval >= CHECKMATE * -1 - 100 || bestEval <= CHECKMATE + 100) break;
			} else {
				break;
			}

			// check time for next iteration
			if (System.currentTimeMillis() - searchStartTime > timeLimitMs * 0.6) break;
		}

		System.out.println("Choice: "+bestMove.toPGNString(gameState.board)+" ("+bestEval+")");
		return bestMove;
	}

	public int evaluatePosition(MoveHandler moveHandler, int depth, int alpha, int beta, boolean allowNullMove){
		if (searchAborted) return 0;
		if (System.currentTimeMillis() - searchStartTime > timeLimitMs) {
			searchAborted = true;
			return 0;
		}

		nodesEvaluated++;

		// quiescence at depth 0
		if (depth <= 0) return quiescenceSearch(moveHandler, alpha, beta, MAX_Q_DEPTH);

		GameState gs = moveHandler.gameState;
		long hash = gs.generateZobristHash();

		// TT probe
		TranspositionTable.SearchState ttEntry = tTable.probe(hash);
		Move ttMove = null;
		if (ttEntry != null) {
			if (ttEntry.hasBestMove) ttMove = new Move(ttEntry.bestMove);
			if (ttEntry.depth >= depth) {
				ttHits++;
				int ttEval = ttEntry.evaluation;
				// adjust mate scores from TT
				if (ttEval > CHECKMATE * -1 - 200) ttEval -= (maxDepth - depth);
				else if (ttEval < CHECKMATE + 200) ttEval += (maxDepth - depth);

				if (ttEntry.boundType == TranspositionTable.EXACT) return ttEval;
				if (ttEntry.boundType == TranspositionTable.LOWER_BOUND && ttEval >= beta) return ttEval;
				if (ttEntry.boundType == TranspositionTable.UPPER_BOUND && ttEval <= alpha) return ttEval;
			}
		}

		int ply = maxDepth - depth;
		int kingPos = (gs.player == Tile.WHITE) ? gs.whiteKingIndex : gs.blackKingIndex;
		boolean inCheck = moveHandler.isAttacked(kingPos, (byte)(gs.player ^ Tile.COLOR));

		// null-move pruning
		if (allowNullMove && !inCheck && depth > 2 && hasNonPawnMaterial(gs.board, gs.player)) {
			GameState nullState = gs.makeNullMove();
			MoveHandler nullHandler = new MoveHandler(nullState);
			int nullEval = -evaluatePosition(nullHandler, depth - 1 - 2, -beta, -beta + 1, false);
			if (nullEval >= beta) return beta;
		}

		List<Move> moves = new ArrayList<>();
		moveHandler.addLegalMoves(moves);

		if (moves.isEmpty()){
			if (inCheck) {
				return CHECKMATE + ply;
			} else {
				return STALEMATE;
			}
		}

		sortMovesWithKillers(moves, gs.board, ttMove, ply);

		int originalAlpha = alpha;
		int bestEvaluation = -INF;
		Move bestMove = null;

		for (Move move : moves){
			moveHandler.tryMove(move);
			int evaluation = -evaluatePosition(moveHandler, depth - 1, -beta, -alpha, true);
			moveHandler.untryMove();

			if (searchAborted) return 0;

			if (evaluation > bestEvaluation){
				bestEvaluation = evaluation;
				bestMove = move;
			}
			if (bestEvaluation > alpha) alpha = bestEvaluation;
			if (alpha >= beta) {
				// store killer and history for non-captures
				if (Tile.piece(gs.board.getTile(move.getTargetIndex())) == Tile.BLANK) {
					if (ply < 64) {
						killerMoves[ply][1] = killerMoves[ply][0];
						killerMoves[ply][0] = move.data;
					}
					byte pieceType = Tile.piece(gs.board.getTile(move.getOriginIndex()));
					if (pieceType < 7) historyTable[pieceType][move.getTargetIndex()] += depth * depth;
				}
				break;
			}
		}

		// store in TT with mate score adjustment
		byte boundType;
		if (bestEvaluation <= originalAlpha) {
			boundType = TranspositionTable.UPPER_BOUND;
		} else if (bestEvaluation >= beta) {
			boundType = TranspositionTable.LOWER_BOUND;
		} else {
			boundType = TranspositionTable.EXACT;
		}

		int storeEval = bestEvaluation;
		if (storeEval > CHECKMATE * -1 - 200) storeEval += (maxDepth - depth);
		else if (storeEval < CHECKMATE + 200) storeEval -= (maxDepth - depth);

		tTable.store(hash, storeEval, depth, bestMove, boundType);

		return bestEvaluation;
	}

	private int quiescenceSearch(MoveHandler moveHandler, int alpha, int beta, int qDepth) {
		quiescenceNodes++;
		int standPat = relativeEvaluation(moveHandler.gameState.board, moveHandler.gameState.player);
		if (standPat >= beta) return beta;
		if (standPat > alpha) alpha = standPat;
		if (qDepth <= 0) return alpha;

		List<Move> captures = new ArrayList<>();
		moveHandler.addCaptureMoves(captures);

		// MVV-LVA sort for captures
		captures.sort((a, b) -> {
			int va = PIECE_VALUES[Tile.piece(moveHandler.gameState.board.getTile(a.getTargetIndex()))] * 10
				- PIECE_VALUES[Tile.piece(moveHandler.gameState.board.getTile(a.getOriginIndex()))];
			int vb = PIECE_VALUES[Tile.piece(moveHandler.gameState.board.getTile(b.getTargetIndex()))] * 10
				- PIECE_VALUES[Tile.piece(moveHandler.gameState.board.getTile(b.getOriginIndex()))];
			return vb - va;
		});

		for (Move move : captures) {
			moveHandler.tryMove(move);
			int evaluation = -quiescenceSearch(moveHandler, -beta, -alpha, qDepth - 1);
			moveHandler.untryMove();

			if (evaluation >= beta) return beta;
			if (evaluation > alpha) alpha = evaluation;
		}
		return alpha;
	}

	private boolean hasNonPawnMaterial(Board board, byte color) {
		for (int i = 0; i < 64; i++) {
			byte tile = board.getTile(i);
			if (Tile.color(tile) == color) {
				byte piece = Tile.piece(tile);
				if (piece == Tile.KNIGHT || piece == Tile.BISHOP || piece == Tile.ROOK || piece == Tile.QUEEN) {
					return true;
				}
			}
		}
		return false;
	}

	private void sortMovesWithKillers(List<Move> moves, Board board, Move ttMove, int ply) {
		moves.sort((a, b) -> scoreMoveWithKillers(b, board, ttMove, ply) - scoreMoveWithKillers(a, board, ttMove, ply));
	}

	private int scoreMoveWithKillers(Move move, Board board, Move ttMove, int ply) {
		if (ttMove != null && move.data == ttMove.data) return 1_000_000;
		byte target = board.getTile(move.getTargetIndex());
		if (Tile.piece(target) != Tile.BLANK) {
			int victimValue = PIECE_VALUES[Tile.piece(target)];
			byte attacker = board.getTile(move.getOriginIndex());
			int attackerValue = PIECE_VALUES[Tile.piece(attacker)];
			return 100_000 + victimValue * 10 - attackerValue;
		}
		if (ply < 64) {
			if (move.data == killerMoves[ply][0]) return 90_000;
			if (move.data == killerMoves[ply][1]) return 80_000;
		}
		byte pieceType = Tile.piece(board.getTile(move.getOriginIndex()));
		if (pieceType < 7) return historyTable[pieceType][move.getTargetIndex()];
		return 0;
	}

	private void sortMoves(List<Move> moves, Board board, Move ttMove, int ply) {
		sortMovesWithKillers(moves, board, ttMove, ply);
	}

	public static int materialEvaluation(Board board, byte color){
		int result = 0;
		for (int i = 0; i < 64; i++){
			byte piece = board.getTile(i);
			if (Tile.color(piece) == color){
				result += PIECE_VALUES[piece&Tile.PIECE];
			}
		}
		return result;
	}
	public static int evaluation(Board board, byte color){
		return materialEvaluation(board, color) + positionEvaluation(board, color)
			+ pawnStructureEvaluation(board, color) + kingSafetyEvaluation(board, color);
	}
	private static int pawnStructureEvaluation(Board board, byte color) {
		int score = 0;
		int[] pawnCount = new int[8]; // count per file
		boolean[] hasPawn = new boolean[8];
		for (int i = 0; i < 64; i++) {
			byte tile = board.getTile(i);
			if (Tile.piece(tile) == Tile.PAWN && Tile.color(tile) == color) {
				int file = i & 7;
				pawnCount[file]++;
				hasPawn[file] = true;
			}
		}
		for (int f = 0; f < 8; f++) {
			if (pawnCount[f] == 0) continue;
			// doubled pawns
			if (pawnCount[f] > 1) score -= 20 * (pawnCount[f] - 1);
			// isolated pawns
			boolean hasNeighbor = (f > 0 && hasPawn[f - 1]) || (f < 7 && hasPawn[f + 1]);
			if (!hasNeighbor) score -= 15 * pawnCount[f];
		}
		// passed pawns
		byte oppColor = (byte)(color ^ Tile.COLOR);
		for (int i = 0; i < 64; i++) {
			byte tile = board.getTile(i);
			if (Tile.piece(tile) != Tile.PAWN || Tile.color(tile) != color) continue;
			int file = i & 7;
			int rank = i >>> 3;
			boolean passed = true;
			int startRank = (color == Tile.WHITE) ? rank + 1 : 0;
			int endRank = (color == Tile.WHITE) ? 7 : rank - 1;
			for (int r = startRank; r <= endRank && passed; r++) {
				for (int df = -1; df <= 1; df++) {
					int f = file + df;
					if (f < 0 || f > 7) continue;
					byte t = board.getTile(f + r * 8);
					if (Tile.piece(t) == Tile.PAWN && Tile.color(t) == oppColor) {
						passed = false;
						break;
					}
				}
			}
			if (passed) {
				int advancedRank = (color == Tile.WHITE) ? rank : 7 - rank;
				score += 10 + 10 * advancedRank;
			}
		}
		return score;
	}
	private static int kingSafetyEvaluation(Board board, byte color) {
		if (isEndgame(board)) return 0;
		int score = 0;
		byte kingIndex = 0;
		for (int i = 0; i < 64; i++) {
			if (board.getTile(i) == (byte)(Tile.KING | color)) { kingIndex = (byte)i; break; }
		}
		int kingFile = kingIndex & 7;
		int kingRank = kingIndex >>> 3;
		// pawn shield
		int shieldRank = (color == Tile.WHITE) ? kingRank + 1 : kingRank - 1;
		if (shieldRank >= 0 && shieldRank < 8) {
			for (int df = -1; df <= 1; df++) {
				int f = kingFile + df;
				if (f < 0 || f > 7) continue;
				byte t = board.getTile(f + shieldRank * 8);
				if (Tile.piece(t) == Tile.PAWN && Tile.color(t) == color) score += 10;
			}
		}
		// open files near king
		for (int df = -1; df <= 1; df++) {
			int f = kingFile + df;
			if (f < 0 || f > 7) continue;
			boolean hasFriendlyPawn = false;
			for (int r = 0; r < 8; r++) {
				byte t = board.getTile(f + r * 8);
				if (Tile.piece(t) == Tile.PAWN && Tile.color(t) == color) {
					hasFriendlyPawn = true;
					break;
				}
			}
			if (!hasFriendlyPawn) score -= 15;
		}
		return score;
	}
	private static final int[] PAWN = {0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] ROOK = {0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0};
	private static final int[] KNIGHT = {-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50};
	private static final int[] BISHOP = {-20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10, -10, 5, 0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20};
	private static final int[] QUEEN = {-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20};
	private static final int[] KING_EARLY = {-30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20};
	private static final int[] KING_LATE = {-50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50};
	private static final int[][] LOOKUP = {new int[0], PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING_EARLY, KING_LATE};
	private static boolean isEndgame(Board board) {
		boolean whiteQueen = false, blackQueen = false;
		int whiteMinors = 0, blackMinors = 0;
		for (int i = 0; i < 64; i++) {
			byte tile = board.getTile(i);
			byte piece = Tile.piece(tile);
			if (piece == Tile.BLANK || piece == Tile.PAWN || piece == Tile.KING) continue;
			if (Tile.color(tile) == Tile.WHITE) {
				if (piece == Tile.QUEEN) whiteQueen = true; else whiteMinors++;
			} else {
				if (piece == Tile.QUEEN) blackQueen = true; else blackMinors++;
			}
		}
		return (!whiteQueen && !blackQueen)
			|| (whiteQueen && whiteMinors <= 1 && blackQueen && blackMinors <= 1);
	}
	public static int positionEvaluation(Board board, byte color){
		boolean endgame = isEndgame(board);
		int result = 0;
		for (int i = 0; i < 64; i++){
			byte piece = board.getTile(i);
			if (Tile.color(piece) == color){
				if (Tile.piece(piece) != Tile.BLANK){
					int[] table;
					if (Tile.piece(piece) == Tile.KING) {
						table = endgame ? KING_LATE : KING_EARLY;
					} else {
						table = LOOKUP[Tile.piece(piece)];
					}
					int j = i;
					if (color == Tile.WHITE){
						j = (j & 0b111) + 56 - 8 * (j >>> 3);
					}
					result += table[j];
				}
			}
		}
		return result;
	}
	public static int relativeEvaluation(Board board, byte color){
		return evaluation(board, color)-evaluation(board, (byte)(color^Tile.COLOR));
	}
}
