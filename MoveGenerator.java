import java.util.List;

public class MoveGenerator {
	public final GameState gameState;
	public MoveGenerator(GameState gameState){
		this.gameState = gameState;
	}
	

	public GameState makeMove(Move move){
		
		GameState upcomingGameState = new GameState(gameState);

		if (move.getOriginIndex() == 0 || move.getTargetIndex() == 0) upcomingGameState.whiteQueenCastle = false;
		if (move.getOriginIndex() == 7 || move.getTargetIndex() == 7) upcomingGameState.whiteKingCastle = false;
		if (move.getOriginIndex() == 4 || move.getTargetIndex() == 4) upcomingGameState.whiteKingCastle = upcomingGameState.whiteQueenCastle = false;
		
		if (move.getOriginIndex() == 56 || move.getTargetIndex() == 56) upcomingGameState.blackQueenCastle = false;
		if (move.getOriginIndex() == 63 || move.getTargetIndex() == 63) upcomingGameState.blackKingCastle = false;
		if (move.getOriginIndex() == 60 || move.getTargetIndex() == 60) upcomingGameState.blackKingCastle = upcomingGameState.blackQueenCastle = false;
		
		upcomingGameState.board.setTile(move.getTargetIndex(), upcomingGameState.board.getTile(move.getOriginIndex()));
		if (move.getFlag() != Move.FLAGLESS){
			int flag = move.getFlag();
			if (flag == Move.PAWN_DOUBLE){
				int d = -8;
				if (Tile.color(upcomingGameState.board.getTile(move.getOriginIndex())) == Tile.WHITE){
					d = 8;
				}
				upcomingGameState.enpassantIndex = move.getTargetIndex() - d;
			} else {
				upcomingGameState.enpassantIndex = -1;
				if (flag == Move.EN_PASSANT_CAPTURE){
					int d = -8;
					if (Tile.color(upcomingGameState.board.getTile(move.getOriginIndex())) == Tile.WHITE){
						d = 8;
					}
					upcomingGameState.board.setTile(move.getTargetIndex() - d, Tile.BLANK);
				} else if ((flag & Move.CASTLING) > 0){
					switch (flag){
						case Move.BLACK_KING_CASTLING -> {
							upcomingGameState.board.setTile(61, Tile.BLACK_ROOK);
							upcomingGameState.board.setTile(63, Tile.BLANK);
						}
						case Move.BLACK_QUEEN_CASTLING -> {
							upcomingGameState.board.setTile(59, Tile.BLACK_ROOK);
							upcomingGameState.board.setTile(56, Tile.BLANK);
						}
						case Move.WHITE_KING_CASTLING -> {
							upcomingGameState.board.setTile(5, Tile.WHITE_ROOK);
							upcomingGameState.board.setTile(7, Tile.BLANK);
						}
						case Move.WHITE_QUEEN_CASTLING -> {
							upcomingGameState.board.setTile(3, Tile.WHITE_ROOK);
							upcomingGameState.board.setTile(0, Tile.BLANK);
						}
					}
				} else if ((flag & Move.PROMOTION) > 0){
					upcomingGameState.board.setTile(move.getTargetIndex(), Move.getPiece(flag)|Tile.color(upcomingGameState.board.getTile(move.getOriginIndex())));
				}
			}
		} else {
			upcomingGameState.enpassantIndex = -1;
		}
		upcomingGameState.board.setTile(move.getOriginIndex(), Tile.BLANK);
		upcomingGameState.player = (byte)(upcomingGameState.player ^ Tile.COLOR);
		
		if (move.getOriginIndex() == upcomingGameState.whiteKingIndex) upcomingGameState.whiteKingIndex = move.getTargetIndex();
		if (move.getOriginIndex() == upcomingGameState.blackKingIndex) upcomingGameState.blackKingIndex = move.getTargetIndex();

		return upcomingGameState;
	}

	public void addLegalMoveForColor(byte color, List<Move> moves){
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

		int kingPos = color == Tile.WHITE ? gameState.whiteKingIndex : gameState.blackKingIndex;
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
			// filter out illegal moves with checks
			Move move = moves.get(i);
			byte originTile = gameState.board.getTile(move.getOriginIndex());
			byte targetTile = gameState.board.getTile(move.getTargetIndex());
			int temporaryKingPosition = kingPos;
			if (move.getOriginIndex() == temporaryKingPosition){
				temporaryKingPosition = move.getTargetIndex();
			}
			//TODO make a try-move and untry move function because of stupid en passanting out of check
			gameState.board.setTile(move.getTargetIndex(), originTile);
			gameState.board.setTile(move.getOriginIndex(), Tile.BLANK);
			if (isAttacked(temporaryKingPosition, (byte)(color^Tile.COLOR))){
				moves.remove(i);
				i--;
			}
			gameState.board.setTile(move.getOriginIndex(), originTile);
			gameState.board.setTile(move.getTargetIndex(), targetTile);
		}
	}
	
	public void addPawnMoves(int x, int y, byte color, List<Move> moves){
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
	public void addRookMoves(int x, int y, byte color, List<Move> moves){
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
	public void addKnightMoves(int x, int y, byte color, List<Move> moves){
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
	public void addBishopMoves(int x, int y, byte color, List<Move> moves){
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
	public void addKingMoves(int x, int y, byte color, List<Move> moves){
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
				if (gameState.whiteQueenCastle && Tile.piece(gameState.board.getTile(1)) == Tile.BLANK && Tile.piece(gameState.board.getTile(2)) == Tile.BLANK && Tile.piece(gameState.board.getTile(3)) == Tile.BLANK && !isAttacked(3, Tile.BLACK)){
					moves.add(new Move(4, 0, 2, 0, Move.WHITE_QUEEN_CASTLING));
				}
				if (gameState.whiteKingCastle && Tile.piece(gameState.board.getTile(5)) == Tile.BLANK && Tile.piece(gameState.board.getTile(6)) == Tile.BLANK && !isAttacked(5, Tile.BLACK)){
					moves.add(new Move(4, 0, 6, 0, Move.WHITE_KING_CASTLING));
				}
			}
		} else {
			if (!isAttacked(60, Tile.WHITE)){
				if (gameState.blackQueenCastle && Tile.piece(gameState.board.getTile(57)) == Tile.BLANK && Tile.piece(gameState.board.getTile(58)) == Tile.BLANK && Tile.piece(gameState.board.getTile(59)) == Tile.BLANK && !isAttacked(59, Tile.WHITE)){
					moves.add(new Move(4, 7, 2, 7, Move.BLACK_QUEEN_CASTLING));
				}
				if (gameState.blackKingCastle && Tile.piece(gameState.board.getTile(61)) == Tile.BLANK && Tile.piece(gameState.board.getTile(62)) == Tile.BLANK && !isAttacked(61, Tile.WHITE)){
					moves.add(new Move(4, 7, 6, 7, Move.BLACK_KING_CASTLING));
				}
			}
		}
	}
	public void addQueenMoves(int x, int y, byte color, List<Move> moves){
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
