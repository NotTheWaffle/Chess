import java.util.ArrayList;
import java.util.List;

public class MoveHandler {
	public final GameState gameState;
	public List<Long> moveHistory;
	public MoveHandler(GameState gameState){
		this.gameState = gameState;
		moveHistory = new ArrayList<>();
	}
	public void makeMove(Move move){
		tryMove(move);
		moveHistory.clear();
	}
	public GameState makeMoveClone(Move move){
		GameState copy = new GameState(gameState);
		copy.makeMove(move);
		return copy;
	}

	public void tryMove(Move move){
		long reversableMove = ((long)(gameState.halfmoveClock & 0xff) << 56) | ((long) gameState.board.getTile(move.getTargetIndex()) << 48) | ((long) (gameState.enpassantIndex & 0xff) << 40) | ((long) gameState.castlingRights << 32) | (long) move.data;
		moveHistory.add(reversableMove);
		gameState.makeMove(move);
	}
	public void untryMove(){
		if (moveHistory.isEmpty()) return;
		gameState.player ^= Tile.COLOR;
		long reversableMove = moveHistory.removeLast();
		Move move = new Move((int) reversableMove);
		byte castlingRights = (byte) ((reversableMove >>> 32) & 0xff);
		byte enpassantIndex = (byte) ((reversableMove >>> 40) & 0xff);
		byte takenTile      = (byte) ((reversableMove >>> 48) & 0xff);
		int halfmoveClock   = (int) ((reversableMove >>> 56) & 0xff);
		gameState.castlingRights = castlingRights;
		gameState.enpassantIndex = enpassantIndex;
		gameState.halfmoveClock = halfmoveClock;
		int originIndex = move.getOriginIndex();
		int targetIndex = move.getTargetIndex();
		
		if (move.getFlag() == Move.FLAGLESS){
			gameState.board.setTile(originIndex, gameState.board.getTile(targetIndex));
			gameState.board.setTile(targetIndex, takenTile);
		} else {
			int flag = move.getFlag();
			if ((flag & Move.PROMOTION) > 0){
				gameState.board.setTile(originIndex, Tile.PAWN | gameState.player);
				gameState.board.setTile(targetIndex, takenTile);
			} else if ((flag & Move.CASTLING) > 0){
				switch (flag){
					case Move.WHITE_KING_CASTLING -> {
						gameState.board.setTile(4, Tile.WHITE_KING);
						gameState.board.setTile(5, Tile.BLANK);
						gameState.board.setTile(6, Tile.BLANK);
						gameState.board.setTile(7, Tile.WHITE_ROOK);
					}
					case Move.WHITE_QUEEN_CASTLING -> {
						gameState.board.setTile(0, Tile.WHITE_ROOK);
						gameState.board.setTile(2, Tile.BLANK);
						gameState.board.setTile(3, Tile.BLANK);
						gameState.board.setTile(4, Tile.WHITE_KING);
					}
					case Move.BLACK_KING_CASTLING -> {
						gameState.board.setTile(60, Tile.BLACK_KING);
						gameState.board.setTile(61, Tile.BLANK);
						gameState.board.setTile(62, Tile.BLANK);
						gameState.board.setTile(63, Tile.BLACK_ROOK);
					}
					case Move.BLACK_QUEEN_CASTLING -> {
						gameState.board.setTile(56, Tile.BLACK_ROOK);
						gameState.board.setTile(58, Tile.BLANK);
						gameState.board.setTile(59, Tile.BLANK);
						gameState.board.setTile(60, Tile.BLACK_KING);
					}
				}
			} else if (flag == Move.EN_PASSANT_CAPTURE){
				int direction = -8;
				if (gameState.player == Tile.WHITE){
					direction = 8;
				}
				gameState.board.setTile(originIndex, Tile.PAWN | gameState.player);
				gameState.board.setTile(targetIndex, Tile.BLANK);
				gameState.board.setTile(targetIndex - direction, Tile.PAWN | (gameState.player ^ Tile.COLOR));
			} else if (flag == Move.PAWN_DOUBLE){
				gameState.board.setTile(originIndex, gameState.board.getTile(targetIndex));
				gameState.board.setTile(targetIndex, Tile.BLANK);
			}
		}
		// Restore king positions: if the king was moved (target == current king index), restore to origin
		if (move.getTargetIndex() == gameState.whiteKingIndex) gameState.whiteKingIndex = (byte) originIndex;
		if (move.getTargetIndex() == gameState.blackKingIndex) gameState.blackKingIndex = (byte) originIndex;
		// For castling, the king was placed at specific squares above, so fix king index explicitly
		if ((move.getFlag() & Move.CASTLING) > 0) {
			switch (move.getFlag()) {
				case Move.WHITE_KING_CASTLING, Move.WHITE_QUEEN_CASTLING -> gameState.whiteKingIndex = 4;
				case Move.BLACK_KING_CASTLING, Move.BLACK_QUEEN_CASTLING -> gameState.blackKingIndex = 60;
			}
		}
	}

	public void addLegalMoves(List<Move> moves){
		addLegalMovesForColor(gameState.player, moves);
	}
	public void addCaptureMoves(List<Move> captures){
		List<Move> allMoves = new ArrayList<>();
		addLegalMoves(allMoves);
		for (Move move : allMoves) {
			if (Tile.piece(gameState.board.getTile(move.getTargetIndex())) != Tile.BLANK
				|| move.getFlag() == Move.EN_PASSANT_CAPTURE) {
				captures.add(move);
			}
		}
	}
	public void addLegalMovesForColor(byte color, List<Move> moves){
		for (int i = 0; i < 64; i++){
			byte tile = gameState.board.getTile(i);
			if (Tile.piece(tile) == Tile.BLANK) continue;
			if (Tile.color(tile) == color){
				addLegalMovesForTile(i, moves);
			}
		}
	}

	public void addLegalMovesForTile(int idx, List<Move> moves){
		int x = idx & 0b111;
		int y = idx >>> 3;
		byte piece = Tile.piece(gameState.board.getTile(idx));
		byte color = Tile.color(gameState.board.getTile(idx));

		switch (piece){
			case Tile.PAWN -> {
				addPawnMoves(x, y, color, moves);
			}
			case Tile.ROOK -> {
				addRookMoves(x, y, color, moves);
			}
			case Tile.BISHOP -> {
				addBishopMoves(x, y, color, moves);
			}
			case Tile.KNIGHT -> {
				addKnightMoves(x, y, color, moves);
			}
			case Tile.KING -> {
				addKingMoves(x, y, color, moves);
			}
			case Tile.QUEEN -> {
				addQueenMoves(x, y, color, moves);
			}
		}
		for (int i = 0; i < moves.size(); i++){
			Move move = moves.get(i);
			tryMove(move);
			int kp = (color == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex;
			boolean illegal = isAttacked(kp, (byte)(color ^ Tile.COLOR));
			untryMove();
			if (illegal) { moves.remove(i); i--; }
		}
	}
	
	private void addPawnMoves(int x, int y, byte color, List<Move> moves){
		if (y == 0 || y == 7) return;
		int dy;
		int dy2;
		boolean doubleMove = false;
		if (color == Tile.WHITE){
			dy = y+1;
			dy2 = y+2;
			if (y == 1) doubleMove = true;
		} else {
			dy = y-1;
			dy2 = y-2;
			if (y == 6) doubleMove = true;
		}
		byte targetTile = gameState.board.getTile(x, dy);
		// single move
		if ((targetTile & Tile.PIECE) == Tile.BLANK){
			if (dy == 0 || dy == 7){
				// promotion
				moves.add(new Move(x, y, x, dy, Move.KNIGHT_PROMOTE));
				moves.add(new Move(x, y, x, dy, Move.ROOK_PROMOTE));
				moves.add(new Move(x, y, x, dy, Move.BISHOP_PROMOTE));
				moves.add(new Move(x, y, x, dy, Move.QUEEN_PROMOTE));
			} else {
				moves.add(new Move(x, y, x, dy));
			}
			// double move
			if (doubleMove){
				targetTile = gameState.board.getTile(x, dy2);
				if ((targetTile & Tile.PIECE) == Tile.BLANK){
					moves.add(new Move(x, y, x, dy2, Move.PAWN_DOUBLE));
				}
			}
		}

		// capture move
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			byte captureTarget = gameState.board.getTile(dx, dy);
			if ((captureTarget & Tile.PIECE) != Tile.BLANK && (captureTarget & Tile.COLOR) != color){
				if (dy == 0 || dy == 7){
					// promotion
					moves.add(new Move(x, y, dx, dy, Move.KNIGHT_PROMOTE));
					moves.add(new Move(x, y, dx, dy, Move.ROOK_PROMOTE));
					moves.add(new Move(x, y, dx, dy, Move.BISHOP_PROMOTE));
					moves.add(new Move(x, y, dx, dy, Move.QUEEN_PROMOTE));
				} else {
					moves.add(new Move(x, y, dx, dy));
				}
			} else if (gameState.enpassantIndex == dx+dy*8){
				moves.add(new Move(x, y, dx, dy, Move.EN_PASSANT_CAPTURE));
			}
		}
	}
	private void addRookMoves(int x, int y, byte color, List<Move> moves){
		byte targetTile;
		// east branch
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.board.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// west branch
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.board.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		
		// north branch
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.board.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
		// south branch
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.board.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
	}
	private void addKnightMoves(int x, int y, byte color, List<Move> moves){
		byte target;
		// vertical rectangle
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-2; dy <= y+2; dy += 4){
				if (dy < 0 || dy >= 8) continue;
				target = gameState.board.getTile(dx, dy);
				if ((target & Tile.PIECE) == Tile.BLANK || (target & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// horizontal rectangle
		for (int dx = x-2; dx <= x+2; dx += 4){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy += 2){
				if (dy < 0 || dy >= 8) continue;
				target = gameState.board.getTile(dx, dy);
				if ((target & Tile.PIECE) == Tile.BLANK || (target & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
	}
	private void addBishopMoves(int x, int y, byte color, List<Move> moves){
		byte target;
		for (int dx = x+1, dy = y+1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx++, dy++){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, dy));
				continue;
			}
			if ((target & Tile.COLOR) != color) moves.add(new Move(x, y, dx, dy));
			break;
		}
		for (int dx = x-1, dy = y+1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx--, dy++){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, dy));
				continue;
			}
			if ((target & Tile.COLOR) != color) moves.add(new Move(x, y, dx, dy));
			break;
		}
		for (int dx = x+1, dy = y-1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx++, dy--){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, dy));
				continue;
			}
			if ((target & Tile.COLOR) != color) moves.add(new Move(x, y, dx, dy));
			break;
		}
		for (int dx = x-1, dy = y-1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx--, dy--){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, dy));
				continue;
			}
			if ((target & Tile.COLOR) != color) moves.add(new Move(x, y, dx, dy));
			break;
		}
	}
	private void addKingMoves(int x, int y, byte color, List<Move> moves){
		byte target;
		for (int dx = x-1; dx <= x+1; dx++){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy++){
				if (dx == x && dy == y) continue;
				if (dy < 0 || dy >= 8) continue;
				target = gameState.board.getTile(dx, dy);
				if ((target & Tile.PIECE) == Tile.BLANK || (target & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// castling
		if (color == Tile.WHITE){
			if (!isAttacked(4, Tile.BLACK)){
				if ((gameState.castlingRights & GameState.CR_WHITE_QUEEN) > 0 && gameState.board.getTile(0) == Tile.WHITE_ROOK && Tile.piece(gameState.board.getTile(1)) == Tile.BLANK && Tile.piece(gameState.board.getTile(2)) == Tile.BLANK && Tile.piece(gameState.board.getTile(3)) == Tile.BLANK && !isAttacked(3, Tile.BLACK)){
					moves.add(new Move(4, 0, 2, 0, Move.WHITE_QUEEN_CASTLING));
				}
				if ((gameState.castlingRights & GameState.CR_WHITE_KING) > 0 && gameState.board.getTile(7) == Tile.WHITE_ROOK && Tile.piece(gameState.board.getTile(5)) == Tile.BLANK && Tile.piece(gameState.board.getTile(6)) == Tile.BLANK && !isAttacked(5, Tile.BLACK)){
					moves.add(new Move(4, 0, 6, 0, Move.WHITE_KING_CASTLING));
				}
			}
		} else {
			if (!isAttacked(60, Tile.WHITE)){
				if ((gameState.castlingRights & GameState.CR_BLACK_QUEEN) > 0 && gameState.board.getTile(56) == Tile.BLACK_ROOK && Tile.piece(gameState.board.getTile(57)) == Tile.BLANK && Tile.piece(gameState.board.getTile(58)) == Tile.BLANK && Tile.piece(gameState.board.getTile(59)) == Tile.BLANK && !isAttacked(59, Tile.WHITE)){
					moves.add(new Move(4, 7, 2, 7, Move.BLACK_QUEEN_CASTLING));
				}
				if ((gameState.castlingRights & GameState.CR_BLACK_KING) > 0 && gameState.board.getTile(63) == Tile.BLACK_ROOK && Tile.piece(gameState.board.getTile(61)) == Tile.BLANK && Tile.piece(gameState.board.getTile(62)) == Tile.BLANK && !isAttacked(61, Tile.WHITE)){
					moves.add(new Move(4, 7, 6, 7, Move.BLACK_KING_CASTLING));
				}
			}
		}
	}
	private void addQueenMoves(int x, int y, byte color, List<Move> moves){
		addRookMoves(x, y, color, moves);
		addBishopMoves(x, y, color, moves);
	}


	public boolean isAttacked(int i, byte color){
		return isAttacked(i%8, i/8, color);
	}
	public boolean isAttacked(int x, int y, byte color){
		byte target;
		// rook/queen checks
		byte threat1 = (byte)(Tile.ROOK|color);
		byte threat2 = (byte)(Tile.QUEEN|color);
		for (int dx = x+1; dx < 8; dx++){
			target = gameState.board.getTile(dx, y);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dx = x-1; dx >= 0; dx--){
			target = gameState.board.getTile(dx, y);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dy = y+1; dy < 8; dy++){
			target = gameState.board.getTile(x, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dy = y-1; dy >= 0; dy--){
			target = gameState.board.getTile(x, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		// bishop/queen checks
		threat1 = (byte)(Tile.BISHOP|color);
		for (int dx = x+1, dy = y+1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx++, dy++){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dx = x-1, dy = y+1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx--, dy++){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dx = x+1, dy = y-1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx++, dy--){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		for (int dx = x-1, dy = y-1; dx >= 0 && dx < 8 && dy >= 0 && dy < 8; dx--, dy--){
			target = gameState.board.getTile(dx, dy);
			if ((target & Tile.PIECE) == Tile.BLANK) continue;
			if (target == threat1 || target == threat2) return true;
			break;
		}
		// knight checks
		threat1 = (byte)(Tile.KNIGHT|color);
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-2; dy <= y+2; dy += 4){
				if (dy < 0 || dy >= 8) continue;
				if (gameState.board.getTile(dx, dy) == threat1) return true;
			}
		}
		for (int dx = x-2; dx <= x+2; dx += 4){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy += 2){
				if (dy < 0 || dy >= 8) continue;
				if (gameState.board.getTile(dx, dy) == threat1) return true;
			}
		}
		// king check
		threat1 = (byte)(Tile.KING|color);
		for (int dx = x-1; dx <= x+1; dx++){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy++){
				if (dx == x && dy == y) continue;
				if (dy < 0 || dy >= 8) continue;
				target = gameState.board.getTile(dx, dy);
				if (target == threat1) return true;
			}
		}
		// pawn check
		int dy;
		if (color == Tile.WHITE){
			dy = y-1;
		} else {
			dy = y+1;
		}
		if (dy >= 0 && dy < 8){
			threat1 = (byte)(Tile.PAWN|color);
			if (x < 7 && gameState.board.getTile(x+1, dy) == threat1){
				return true;
			}
			if (x > 0 && gameState.board.getTile(x-1, dy) == threat1){
				return true;
			}
		}
		return false;
	}
}
