
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Sprites{
	public static final String PIECE_STYLE = "Crude2";
	public static final BufferedImage PAWN_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/pawnWhite.png");
	public static final BufferedImage ROOK_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/rookWhite.png");
	public static final BufferedImage KNIGHT_WHITE = loadImage("Pieces/"+PIECE_STYLE+"/knightWhite.png");
	public static final BufferedImage BISHOP_WHITE = loadImage("Pieces/"+PIECE_STYLE+"/bishopWhite.png");
	public static final BufferedImage KING_WHITE   = loadImage("Pieces/"+PIECE_STYLE+"/kingWhite.png");
	public static final BufferedImage QUEEN_WHITE  = loadImage("Pieces/"+PIECE_STYLE+"/queenWhite.png");

	public static final BufferedImage PAWN_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/pawnBlack.png");
	public static final BufferedImage ROOK_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/rookBlack.png");
	public static final BufferedImage KNIGHT_BLACK = loadImage("Pieces/"+PIECE_STYLE+"/knightBlack.png");
	public static final BufferedImage BISHOP_BLACK = loadImage("Pieces/"+PIECE_STYLE+"/bishopBlack.png");
	public static final BufferedImage KING_BLACK   = loadImage("Pieces/"+PIECE_STYLE+"/kingBlack.png");
	public static final BufferedImage QUEEN_BLACK  = loadImage("Pieces/"+PIECE_STYLE+"/queenBlack.png");

	public static final String BOARD_STYLE = "Clean";
	public static final BufferedImage BOARD_WHITE   = loadImage("Boards/"+BOARD_STYLE+"/boardWhite.png");
	public static final BufferedImage BOARD_BLACK  = loadImage("Boards/"+BOARD_STYLE+"/boardBlack.png");
	
	private Sprites(){}
	private static final BufferedImage[] PIECES = {
		null, PAWN_BLACK, ROOK_BLACK, KNIGHT_BLACK, BISHOP_BLACK, QUEEN_BLACK, KING_BLACK, null,
		null, PAWN_WHITE, ROOK_WHITE, KNIGHT_WHITE, BISHOP_WHITE, QUEEN_WHITE, KING_WHITE, null,
	};
	public static final Color[] COLORS = {
		null, Color.DARK_GRAY, Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.PINK, null,
		null, Color.DARK_GRAY, Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.PINK, null,
	};

	public static BufferedImage getImage(byte b){
		if ((b & Tile.PIECE) == Tile.BLANK) return null;
		return PIECES[b];
	}
	public static BufferedImage loadImage(String filePath){
		BufferedImage image = null;
		try {image = ImageIO.read(new File("Assets/"+filePath));} catch (IOException e) {System.out.println("failed to load "+filePath);}
		return image;
	}
}
