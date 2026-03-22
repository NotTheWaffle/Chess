import java.awt.Color;
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
	public ChessGame(boolean whiteBot, boolean blackBot, boolean rotate){
		this(whiteBot, blackBot, rotate, null);
	}
	public ChessGame(boolean whiteBot, boolean blackBot, boolean rotate, String fenString){
		super(256, 256);
		sprites = new Sprites("Indexed", "Clean");
		gameState = fenString == null ? new GameState() : new GameState(fenString);
		moves = new ArrayList<>();
		moveHandler = new MoveHandler(gameState);
		promoting = false;
		promotingColor = 0;
		promotions = new ArrayList<>();
		lastMove = null;
		playerWhite = whiteBot ? new Agent(4) : null;
		playerBlack = blackBot ? new Agent(4) : null;
		callAgentPlay();
		rotatedRender = rotate;
	}
	@Override
	public void tick(){}
	@Override
	public void onMouseDown(){
		System.out.println("mouse down");
		int tileX = input.mouseX/32;
		int tileY = input.mouseY/32;
		if (!rotatedRender){
			tileX = 7-tileX;
		}
		if (rotatedRender){
			tileY = 7-tileY;
		}
		if (tileX < 0) tileX = 0;
		if (tileX > 7) tileX = 7;
		if (tileY < 0) tileY = 0;
		if (tileY > 7) tileY = 7;
		int mouseIndex = tileX + tileY*8;

		if (promoting) return;


		for (Move move : moves){
			if (move.getTargetIndex() == mouseIndex){
				System.out.println("playing a move...");
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
		System.out.println("mouse up");
		int tileX = input.mouseX/32;
		int tileY = input.mouseY/32;
		if (!rotatedRender){
			tileX = 7-tileX;
		}
		if (rotatedRender){
			tileY = 7-tileY;
		}
		if (tileX < 0) tileX = 0;
		if (tileX > 7) tileX = 7;
		if (tileY < 0) tileY = 0;
		if (tileY > 7) tileY = 7;
		int mouseIndex = tileX + tileY*8;
		
		if (promoting){
			int index;
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
			gameState.makeMove(lastMove);

			moves.clear();
			promoting = false;
			promotions = new ArrayList<>();
			promotingColor = 0;

			callAgentPlay();
		} else {
			List<Move> chosen = new ArrayList<>();
			for (Move move : moves){
				if (move.getTargetIndex() == mouseIndex){
					chosen.add(move);
				}
			}
			if (chosen.size() == 1){
				lastMove = chosen.get(0);
				gameState.makeMove(lastMove);
				moves.clear();
				callAgentPlay();
			} else if (chosen.size() > 1){
				promoting = true;
				promotions = chosen;
				promotingColor = Tile.color(gameState.board.getTile(chosen.get(0).getOriginIndex()));
			}
		}
		render();
	}
	public void render(){
		window.render();
	}
	public void callAgentPlay(){
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
			try {Thread.sleep(500);} catch (InterruptedException e){}
			long start = System.nanoTime();
			Move move = playerF.findBestMove(gameState);
			System.out.println((System.nanoTime()-start)/1_000_000.0);
			lastMove = move;
			gameState.makeMove(lastMove);
			callAgentPlay();
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
			//	g2d.drawString(piece+" ", transformX(x), transformY(y)+32);
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
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(0).getFlag()))), transformX(dx), transformY(dy)*32, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(1).getFlag()))), transformX(dx), transformY(dy)*32+32*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(2).getFlag()))), transformX(dx), transformY(dy)*32+64*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(3).getFlag()))), transformX(dx), transformY(dy)*32+96*m, null);
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
			g2d.fillOval(transformX(dx)+8, transformY(dy)+8, 16, 16);
		}
	}
	@Override
	public String name(){
		return "Chess Game";
	}
}
