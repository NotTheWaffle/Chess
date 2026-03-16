import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class ChessGame extends Game{
	public final byte humanColor;
	private final boolean rotatedRender;
	private final GameState gameState;
	private final List<Move> moves;
	private final MoveGenerator moveGenerator;
	private boolean promoting;
	private byte promotingColor;
	private List<Move> promotions;
	private Move lastMove;
	private final Agent aiPlayer;
	private final Sprites spriteSet;

	public ChessGame(byte humanColor, boolean flip){
		super(256, 256);
		spriteSet = new Sprites("Indexed", "Crude2");
		gameState = new GameState();
		moves = new ArrayList<>();
		moveGenerator = new MoveGenerator(gameState);
		promoting = false;
		promotingColor = 0;
		promotions = new ArrayList<>();
		lastMove = null;
		this.humanColor = humanColor;
		boolean rotatedRenderTemp = true;
		if (humanColor == Tile.BLACK){
			rotatedRenderTemp = false;
			callAgentPlay();
		}
		rotatedRender = rotatedRenderTemp ^ flip;
		aiPlayer = new Agent(4);
	}
	@Override
	public void tick(){}
	@Override
	public void onMouseDown(){
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
				return;
			}
		}
		moves.clear();
		if ((gameState.board.getTile(mouseIndex) & Tile.COLOR) == humanColor && gameState.player == humanColor){
			moveGenerator.addLegalMovesForTile(mouseIndex, moves);
		}
		render();
	}
	@Override
	public void onMouseUp(){
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
		Thread thread = new Thread(() -> {
			try {Thread.sleep(100);} catch (InterruptedException e){}
			long start = System.nanoTime();
			Move move = aiPlayer.findBestMove(gameState);
			System.out.println((System.nanoTime()-start)/1_000_000.0);
			lastMove = move;
			gameState.makeMove(lastMove);
			render();
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
			g2d.drawImage(spriteSet.BOARD_WHITE, 0, 0, null);
		} else {
			g2d.drawImage(spriteSet.BOARD_BLACK, 0, 0, null);
		}
		for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				byte piece = board.getTile(x, y);
				g2d.drawImage(spriteSet.getImage(piece), transformX(x), transformY(y), null);
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
			g2d.drawImage(spriteSet.getImage((byte) (promotingColor|Move.getPiece(promotions.get(0).getFlag()))), transformX(dx), transformY(dy)*32, null);
			g2d.drawImage(spriteSet.getImage((byte) (promotingColor|Move.getPiece(promotions.get(1).getFlag()))), transformX(dx), transformY(dy)*32+32*m, null);
			g2d.drawImage(spriteSet.getImage((byte) (promotingColor|Move.getPiece(promotions.get(2).getFlag()))), transformX(dx), transformY(dy)*32+64*m, null);
			g2d.drawImage(spriteSet.getImage((byte) (promotingColor|Move.getPiece(promotions.get(3).getFlag()))), transformX(dx), transformY(dy)*32+96*m, null);
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
			g2d.setColor(spriteSet.COLORS[board.getTile(move.getOriginIndex())].darker());
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
