import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import javax.swing.JPanel;

public class GamePanel extends JPanel{

	private final Game game;
	private final Input input;

	public boolean paused;
	public double scaling;
	public int offsetX;
	public int offsetY;
	
	public GamePanel(final int width, final int height, final Window window, final Game game){
		this.game = game;
		this.input = game.input;
		scaling = 1;
		this.setFocusTraversalKeysEnabled(false);
		this.setFocusable(true);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				updateMousePosition(e);
				input.mouseDown &= ~(1<<(e.getButton()-1));
				game.onMouseUp();
			}
			@Override
			public void mousePressed(final MouseEvent e) {
				updateMousePosition(e);
				input.mouseDown |= (1<<(e.getButton()-1));
				game.onMouseDown();
			}
			@Override
			public void mouseEntered(final MouseEvent e) {
				updateMousePosition(e);
			}
			@Override
			public void mouseExited(final MouseEvent e) {
				updateMousePosition(e);
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				updateMousePosition(e);
			}
			@Override
			public void mouseDragged(final MouseEvent e) {
				updateMousePosition(e);
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(final MouseWheelEvent e){
				input.mouseWheel += e.getPreciseWheelRotation();
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e){
				if (e.getKeyCode() < 256) input.keys[e.getKeyCode()] = true;
				game.onKeyDown(e.getKeyCode());
			}
			@Override
			public void keyReleased(final KeyEvent e){
				if (e.getKeyCode() < 256) input.keys[e.getKeyCode()] = false;
			}
		});
	}
	private void updateMousePosition(final MouseEvent e){
		input.mouseX = (int) ((e.getX()-offsetX)/scaling);
		input.mouseY = (int) ((e.getY()-offsetY)/scaling);
	}
	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		final Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(offsetX, offsetY);
		g2d.scale(scaling, scaling);
		game.updateFrame(g2d);

		g2d.dispose();
	}
}