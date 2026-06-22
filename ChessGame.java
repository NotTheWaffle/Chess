import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class ChessGame extends Game{
	private final boolean rotatedRender;
	private final GameState gameState;
	private final List<Move> moves;
	private final MoveHandler moveHandler;

	private boolean promoting;
	private byte promotingColor;
	private List<Move> promotions;

	private Move lastMove;
	private final Agent playerWhite;
	private final Agent playerBlack;
	private final Sprites sprites;

	private boolean gameOver;
	private String gameResult;
	private final List<Long> positionHistory;
	private final List<GameState> stateHistory;
	private final List<String> moveNotations;
	private int moveNumber;
	public ChessGame(boolean whiteBot, boolean blackBot, boolean rotate){
		this(whiteBot, blackBot, rotate, null);
	}
	public ChessGame(boolean whiteBot, boolean blackBot, boolean rotate, String fenString){
		super(256, 256);
		sprites = new Sprites("Clean", "Clean");
		gameState = fenString == null ? new GameState() : new GameState(fenString);
		moves = new ArrayList<>();
		moveHandler = new MoveHandler(gameState);
		promoting = false;
		promotingColor = 0;
		promotions = new ArrayList<>();
		lastMove = null;
		playerWhite = whiteBot ? new Agent(20, 5000) : null;
		playerBlack = blackBot ? new Agent(20, 5000) : null;
		rotatedRender = rotate;
		gameOver = false;
		gameResult = "";
		positionHistory = new ArrayList<>();
		positionHistory.add(gameState.generateZobristHash());
		stateHistory = new ArrayList<>();
		moveNotations = new ArrayList<>();
		moveNumber = 1;
	}
	public void start(){
		callAgentPlay();
	}
	@Override
	public void tick(){}
	@Override
	public void onMouseDown(){
		if (gameOver) return;
		render();
		int tileX = input.mouseX/32;
		int tileY = input.mouseY/32;
		if (!rotatedRender){
			tileX = 7-tileX;
		}
		if (rotatedRender){
			tileY = 7-tileY;
		}
		if (tileX < 0) return;
		if (tileX > 7) return;
		if (tileY < 0) return;
		if (tileY > 7) return;
		int mouseIndex = tileX + tileY*8;

		if (promoting) return;


		for (Move move : moves){
			if (move.getTargetIndex() == mouseIndex){
				return;
			}
		}
		moves.clear();
		if ((gameState.board.getTile(mouseIndex) & Tile.COLOR) == gameState.player && ((playerWhite == null && gameState.player == Tile.WHITE) || (playerBlack == null && gameState.player == Tile.BLACK))){
			moveHandler.addLegalMovesForTile(mouseIndex, moves);
		}
		render();
	}
	@Override
	public void onMouseUp(){
		if (gameOver) return;
		int tileX = input.mouseX/32;
		int tileY = input.mouseY/32;
		if (!rotatedRender){
			tileX = 7-tileX;
		}
		if (rotatedRender){
			tileY = 7-tileY;
		}
		if (tileX < 0) return;
		if (tileX > 7) return;
		if (tileY < 0) return;
		if (tileY > 7) return;
		int mouseIndex = tileX + tileY*8;
		
		if (promoting){
			int index;
			if (tileX != promotions.get(0).getTargetX()){
				return;
			}
			if (promotingColor == Tile.WHITE){
				if (tileY < 4){
					return;
				}
				index = 7-tileY;
			} else {
				if (tileY > 3){
					return;
				}
				index = tileY;
			}
			lastMove = promotions.get(index);
			stateHistory.add(new GameState(gameState));
			recordMove(lastMove);
			gameState.makeMove(lastMove);
			positionHistory.add(gameState.generateZobristHash());

			moves.clear();
			promoting = false;
			promotions = new ArrayList<>();
			promotingColor = 0;

			checkGameEnd();
			if (!gameOver) callAgentPlay();
		} else {
			List<Move> chosen = new ArrayList<>();
			for (Move move : moves){
				if (move.getTargetIndex() == mouseIndex){
					chosen.add(move);
				}
			}
			if (chosen.size() == 1){
				lastMove = chosen.get(0);
				stateHistory.add(new GameState(gameState));
				recordMove(lastMove);
				gameState.makeMove(lastMove);
				positionHistory.add(gameState.generateZobristHash());
				moves.clear();
				checkGameEnd();
				if (!gameOver) callAgentPlay();
			} else if (chosen.size() > 1){
				promoting = true;
				promotions = chosen;
				promotingColor = Tile.color(gameState.board.getTile(chosen.get(0).getOriginIndex()));
			}
		}
		render();
	}
	private boolean isThreefoldRepetition() {
		long current = positionHistory.getLast();
		int count = 0;
		for (long hash : positionHistory) {
			if (hash == current && ++count >= 3) return true;
		}
		return false;
	}
	private void checkGameEnd() {
		if (gameOver) return;
		if (gameState.isFiftyMoveRule()) {
			gameOver = true;
			gameResult = "Draw by 50-move rule";
			System.out.println(gameResult);
			return;
		}
		if (isThreefoldRepetition()) {
			gameOver = true;
			gameResult = "Draw by threefold repetition";
			System.out.println(gameResult);
			return;
		}
		if (gameState.isInsufficientMaterial()) {
			gameOver = true;
			gameResult = "Draw by insufficient material";
			System.out.println(gameResult);
			return;
		}
		List<Move> legalMoves = new ArrayList<>();
		moveHandler.addLegalMoves(legalMoves);
		if (legalMoves.isEmpty()) {
			int kingPos = (gameState.player == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex;
			if (moveHandler.isAttacked(kingPos, (byte)(gameState.player ^ Tile.COLOR))) {
				String winner = (gameState.player == Tile.WHITE) ? "Black" : "White";
				gameOver = true;
				gameResult = "Checkmate! " + winner + " wins";
			} else {
				gameOver = true;
				gameResult = "Stalemate! Draw";
			}
			System.out.println(gameResult);
		}
	}
	private void recordMove(Move move) {
		String notation = move.toPGNString(gameState);
		moveNotations.add(notation);
		if (gameState.player == Tile.WHITE) {
			System.out.print(moveNumber + ". " + notation + " ");
		} else {
			System.out.println(notation);
			moveNumber++;
		}
	}
	private void undoMove() {
		if (stateHistory.isEmpty()) return;
		// undo 1 move
		GameState prev = stateHistory.removeLast();
		gameState.board = prev.board;
		gameState.player = prev.player;
		gameState.enpassantIndex = prev.enpassantIndex;
		gameState.castlingRights = prev.castlingRights;
		gameState.halfmoveClock = prev.halfmoveClock;
		gameState.whiteKingIndex = prev.whiteKingIndex;
		gameState.blackKingIndex = prev.blackKingIndex;
		positionHistory.removeLast();
		moveNotations.removeLast();
		if (gameState.player == Tile.BLACK && moveNumber > 1) moveNumber--;
		lastMove = null;
		moves.clear();
		gameOver = false;
		gameResult = "";
	}
	@Override
	public void onKeyDown(int keyCode) {
		if (keyCode == java.awt.event.KeyEvent.VK_U) {
			// undo: if playing vs bot, undo 2 moves (bot + player)
			boolean vsBot = (playerWhite != null) != (playerBlack != null);
			undoMove();
			if (vsBot && !stateHistory.isEmpty()) undoMove();
			render();
		} else if (keyCode == java.awt.event.KeyEvent.VK_R) {
			if (!gameOver) {
				String loser = (gameState.player == Tile.WHITE) ? "White" : "Black";
				gameOver = true;
				gameResult = loser + " resigns";
				System.out.println("\n" + gameResult);
				render();
			}
		}
	}
	public void render(){
		window.render();
	}
	private void callAgentPlay(){
		if (gameOver) return;
		Agent player = null;
		if (playerWhite != null && gameState.player == Tile.WHITE){
			player = playerWhite;
		} else if (playerBlack != null && gameState.player == Tile.BLACK){
			player = playerBlack;
		}
		if (player == null) return;

		final Agent playerF = player;
		Thread thread = new Thread(() -> {
			System.out.println("bot thinking");
			long start = System.nanoTime();
			Move move = playerF.findBestMove(gameState);
			System.out.println("Bot thought for: "+(System.nanoTime()-start)/1_000_000.0+"ms");
			if (move == null) { checkGameEnd(); render(); return; }
			lastMove = move;
			stateHistory.add(new GameState(gameState));
			recordMove(lastMove);
			gameState.makeMove(lastMove);
			positionHistory.add(gameState.generateZobristHash());
			checkGameEnd();
			if (!gameOver) callAgentPlay();
			render();
			System.out.println("bot done thinking");
		});
		thread.start();
	}
	public int transformX(int tileX){
		if (rotatedRender){
			return tileX*32;
		} else {
			return 224-tileX*32;
		}
	}
	public int transformY(int tileY){
		if (rotatedRender){
			return 224-tileY*32;
		} else {
			return tileY*32;
		}
	}
	@Override
	public void updateFrame(Graphics2D g2d){
		Board board = gameState.board;
		if (rotatedRender){
			g2d.drawImage(sprites.BOARD_WHITE, 0, 0, null);
		} else {
			g2d.drawImage(sprites.BOARD_BLACK, 0, 0, null);
		}
		for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				byte piece = board.getTile(x, y);
				g2d.drawImage(sprites.getImage(piece), transformX(x), transformY(y), null);
			}
		}
		// promotion dialogue
		if (promoting) {
			int dx = promotions.get(0).getTargetX();
			int dy = promotions.get(0).getTargetY();
			g2d.setColor(Color.GRAY);
			int m;
			if (promotingColor == Tile.WHITE){
				g2d.fillRect(transformX(dx), transformY(dy), 32, 128);
				m = 1;
			} else {
				g2d.fillRect(transformX(dx), transformY(dy)-96, 32, 128);
				m = -1;
			}
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(0).getFlag()))), transformX(dx), transformY(dy), null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(1).getFlag()))), transformX(dx), transformY(dy)+32*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(2).getFlag()))), transformX(dx), transformY(dy)+64*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(3).getFlag()))), transformX(dx), transformY(dy)+96*m, null);
			return;
		}
		// show previous move arrow
		if (lastMove != null){
			g2d.setColor(Color.RED);
			int ox = lastMove.getOriginX();
			int oy = lastMove.getOriginY();
			int dx = lastMove.getTargetX();
			int dy = lastMove.getTargetY();
			g2d.drawLine(transformX(dx)+16, transformY(dy)+16, transformX(ox)+16, transformY(oy)+16);
		}
		// show possible moves
		for (Move move : moves){
			g2d.setColor(sprites.COLORS[board.getTile(move.getOriginIndex())].darker());
			int dx = move.getTargetX();
			int dy = move.getTargetY();
			g2d.fillRect(transformX(dx)+12, transformY(dy)+12, 8, 8);
		}
		// game over overlay
		if (gameOver) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 96, 256, 64);
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font("Arial", Font.BOLD, 14));
			int textWidth = g2d.getFontMetrics().stringWidth(gameResult);
			g2d.drawString(gameResult, (256 - textWidth) / 2, 134);
		}
	}
	@Override
	public String name(){
		return "Chess Game";
	}
}
