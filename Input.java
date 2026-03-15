
import java.util.Arrays;

public class Input {
	public static final int MOUSE_LEFT = 1;
	public static final int MOUSE_MIDDLE = 2;
	public static final int MOUSE_RIGHT = 4;
	public static final int MOUSE_4 = 8;
	public static final int MOUSE_5 = 16;


	public static final int BACKSPACE = 0x08;
	public static final int TAB = 0x09;
	public static final int ENTER = 0x0a;
	
	public static final int SHIFT = 0x10;
	public static final int CTRL = 0x11;
	public static final int ALT = 0x12;
	public static final int CAPS_LOCK = 0x14;
	public static final int ESCAPE = 0x1b;


	public static final int PAGE_UP = 0x21;
	public static final int PAGE_DOWN = 0x22;
	public static final int END = 0x23;
	public static final int HOME = 0x24;

	public static final int INSERT = 0x2d;
	public static final int DELETE = 0x2e;

	public static final int LEFT_ARROW = 0x25;
	public static final int UP_ARROW = 0x26;
	public static final int RIGHT_ARROW = 0x27;
	public static final int DOWN_ARROW = 0x28;

	public static final int SINGLE_QUOTE = 0xde;
	public static final int BACKTICK = 0xc0;

	public int mouseX;
	public int mouseY;

	public int mouseDown;

	public double mouseWheel;

	public final boolean[] keys;

	public Input(final Game game) {
		keys = new boolean[256];
		reset();
	}

	public final void reset(){
		mouseX = -1;
		mouseY = -1;
		mouseDown = 0;
		mouseWheel = 0;
		Arrays.fill(keys, false);
	}
}