import java.awt.Graphics2D;

public abstract class Game {
	public final Input input;
	public Window window;

	public final int width;
	public final int height;

	protected Game(final int width, final int height){
		input = new Input(this);
		this.width = width;
		this.height = height;
	}
	public abstract String name();
	public abstract void tick();
	public abstract void onMouseUp();
	public abstract void onMouseDown();
	public abstract void updateFrame(final Graphics2D g2d);
}