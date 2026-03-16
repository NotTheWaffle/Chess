
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Sprites{
	public final String PIECE_STYLE;
	public final BufferedImage PAWN_WHITE;
	public final BufferedImage ROOK_WHITE;
	public final BufferedImage KNIGHT_WHITE;
	public final BufferedImage BISHOP_WHITE;
	public final BufferedImage KING_WHITE;
	public final BufferedImage QUEEN_WHITE;

	public final BufferedImage PAWN_BLACK;
	public final BufferedImage ROOK_BLACK;
	public final BufferedImage KNIGHT_BLACK;
	public final BufferedImage BISHOP_BLACK;
	public final BufferedImage KING_BLACK;
	public final BufferedImage QUEEN_BLACK;

	public final String BOARD_STYLE;
	public final BufferedImage BOARD_WHITE;
	public final BufferedImage BOARD_BLACK;
	
	public Sprites(String boardStyle, String pieceStyle){
		this.PIECE_STYLE = pieceStyle;
		this.BOARD_STYLE = boardStyle;
		PAWN_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/pawnBlack.png");
		ROOK_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/rookBlack.png");
		KNIGHT_BLACK = loadImage("Pieces/"+PIECE_STYLE+"/knightBlack.png");
		BISHOP_BLACK = loadImage("Pieces/"+PIECE_STYLE+"/bishopBlack.png");
		KING_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/kingBlack.png");
		QUEEN_BLACK  = loadImage("Pieces/"+PIECE_STYLE+"/queenBlack.png");
		
		PAWN_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/pawnWhite.png");
		ROOK_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/rookWhite.png");
		KNIGHT_WHITE = loadImage("Pieces/"+PIECE_STYLE+"/knightWhite.png");
		BISHOP_WHITE = loadImage("Pieces/"+PIECE_STYLE+"/bishopWhite.png");
		KING_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/kingWhite.png");
		QUEEN_WHITE  = loadImage("Pieces/"+PIECE_STYLE+"/queenWhite.png");

		BOARD_WHITE  = loadImage("Boards/"+BOARD_STYLE+"/boardWhite.png");
		BOARD_BLACK  = loadImage("Boards/"+BOARD_STYLE+"/boardBlack.png");
		PIECES = new BufferedImage[] {
			null, PAWN_BLACK, ROOK_BLACK, KNIGHT_BLACK, BISHOP_BLACK, QUEEN_BLACK, KING_BLACK, null,
			null, PAWN_WHITE, ROOK_WHITE, KNIGHT_WHITE, BISHOP_WHITE, QUEEN_WHITE, KING_WHITE, null,
		};
	}
	private final BufferedImage[] PIECES;
	public final Color[] COLORS = {
		null, Color.DARK_GRAY, Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.PINK, null,
		null, Color.DARK_GRAY, Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.PINK, null,
	};

	public BufferedImage getImage(byte b){
		if ((b & Tile.PIECE) == Tile.BLANK) return null;
		return PIECES[b];
	}
	public static BufferedImage loadImage(String filePath){
		BufferedImage image = null;
		try {image = ImageIO.read(new File("Assets/"+filePath));} catch (IOException e) {System.out.println("failed to load "+filePath);}
		return image;
	}
}
