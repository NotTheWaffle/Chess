import java.awt.event.*;
import javax.swing.*;

public class Window {
	
	private final JFrame frame;

	private final GamePanel gamePanel;
	private final Input input;

	public Window(final Game game){
		this.input = game.input;
		final int width  = 8  + game.width  + 8;
		final int height = 31 + game.height + 8;

		frame = new JFrame(game.name());
		frame.setBounds(0, 0, width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setLayout(null);
		frame.setResizable(true);
		

		gamePanel = new GamePanel(game.width, game.height, this, game);
		gamePanel.setBounds(0, 0, game.width, game.height);
		frame.add(gamePanel);
		
		
		frame.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e){gamePanel.paused = false;}
			@Override
			public void focusLost(FocusEvent e){gamePanel.paused = true; input.reset();}
		});

		frame.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e){
				gamePanel.setSize(e.getComponent().getWidth(), e.getComponent().getHeight());
				gamePanel.scaling = Math.min((gamePanel.getWidth()-16)/(double)game.width,(gamePanel.getHeight()-39)/(double)game.height);
				gamePanel.offsetX = (int) ((getWidth()  - (game.width  * gamePanel.scaling))/2);
				gamePanel.offsetY = (int) ((getHeight() - (game.height * gamePanel.scaling))/2);
			}
		});

		gamePanel.requestFocus();
		// doing this at the end so its mostly constructed
		game.window = this;
	}

	public int getWidth(){
		return frame.getWidth()-16;
	}

	public int getHeight(){
		return frame.getHeight()-39;
	}

	public void render(){
		gamePanel.repaint();
	}
}