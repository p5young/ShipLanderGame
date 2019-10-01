import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.geom.*;
import javax.vecmath.*;

// the actual game view
public class PlayView extends JPanel implements Observer {

	GameModel model;

    public PlayView(GameModel model) {

    	this.model = model;
    	model.addObserver(this);

        // needs to be focusable for keylistener
        setFocusable(true);

        // Key Listener
        this.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                //System.out.println(e.getKeyChar());
                switch (e.getKeyChar()) {
                	case 'w':
                		model.ship.thrustUp();
                		break;
                	case 'a':
                		model.ship.thrustLeft();
                		break;
                	case 's':
                		model.ship.thrustDown();
                		break;
                	case 'd':
                		model.ship.thrustRight();
                		break;
                	case ' ':
                		if (model.crashed) {
                			model.crashed = false;
                			model.ship.reset(model.ship.startPosition);
                		} else if (model.landed) {
                			model.landed = false;
                			model.ship.reset(model.ship.startPosition);
                		} else {
                			model.ship.setPaused( ! model.ship.isPaused());	
                		}
                }
            }
        }); // Key Listener

        // want the background to be black
        setBackground(Color.BLACK);

    }

    @Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int width = this.getWidth();
		int height = this.getHeight();
		int shipX = (int)model.ship.getPosition().getX();
		int shipY = (int)model.ship.getPosition().getY();

		Graphics2D g2 = (Graphics2D) g; // cast to get 2D drawing methods

		// save the current g2 transform matrix 
        AffineTransform backup = g2.getTransform();
        AffineTransform M = new AffineTransform(backup);
        // Scale
        AffineTransform S = AffineTransform.getScaleInstance(3.0f, 3.0f);
        // Translate - width / 2 becomes width / 6 in the translate matrix due to 3x scaling
        AffineTransform T = AffineTransform.getTranslateInstance(-(shipX - (width / 6)), -(shipY - (height / 6)));
        // Apply transformations
        M.concatenate(S);
        M.concatenate(T);
        g2.setTransform(M);

        // Draw World
		Rectangle2D.Double worldBounds = model.getWorldBounds();
		g2.setColor(Color.lightGray);
		g2.fillRect(0, 0, (int)worldBounds.width, (int)worldBounds.height);
		// Draw Terrain
		g2.setColor(Color.darkGray);
		g2.fillPolygon(model.terrain);
		// Draw Landing Pad
		g2.setColor(Color.red);
		g2.fillRect(model.padPos.x, model.padPos.y, 40, 10);
		// Draw Ship
		g2.setColor(Color.blue);
		g2.fillRect(shipX - 5, shipY - 5, 10, 10);
		g2.setTransform(backup);
	}

    @Override
    public void update(Observable o, Object arg) {
    	repaint();
    }
}
