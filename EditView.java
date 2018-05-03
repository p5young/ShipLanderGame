import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

// the editable view of the terrain and landing pad
public class EditView extends JPanel implements Observer {

	// Constructor
    public EditView(GameModel model) {

    	this.model = model;

    	model.addObserver(this);

    	padDrag = false;
    	terrainMove = false;

    	// Mouse Listener
    	this.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
            	if (e.getClickCount() == 2)
            		model.movePad(e);
            }
            public void mousePressed(MouseEvent e) {
            	System.out.println("Pressed");
            	if (padHittest(e.getX(), e.getY())) {
            		model.startPadMove(e.getX(), e.getY());
            		padDrag = true;
            	} else if (terrainHittest(e.getX(), e.getY())) {
            		model.startTerrainMove(e.getY(), terrainIndex);
            		terrainMove = true;
            	}
            }
            public void mouseReleased(MouseEvent e) {
            	System.out.println("Released");
            	if (padDrag) {
            		padDrag = false;
            		model.finishPadMove();
            	} else if (terrainMove) {
            		terrainMove = false;
            		model.finishTerrainMove();
            	}
            }
        });

    	// Mouse Motion Listener
        this.addMouseMotionListener(new MouseAdapter(){
            public void mouseDragged(MouseEvent e) {
            	if (padDrag)
            		model.padDrag(e.getX(), e.getY());
            	else if (terrainMove)
            		model.terrainMove(e.getY());
            }
        });

        // want the background to be black
        setBackground(Color.BLACK);

    } // Constructor

    GameModel model;

    // true after hittest on landing pad, used for click & drag
    private boolean padDrag;

    private boolean padHittest(int x, int y) {
    	int xmin = model.padPos.x;
    	int xmax = model.padPos.x + 40;
    	int ymin = model.padPos.y;
    	int ymax = model.padPos.y + 10;
    	return (xmin <= x && x <= xmax && ymin <= y && y <= ymax);
    }

    // Used to indicate which terrain peak/ valley was clicked - used for click & drag
    private int terrainIndex;

    private boolean terrainMove;

    private boolean terrainHittest(int x, int y) {
    	for (int i = 0 ; i < 20 ; ++i) {
    		if (Math.abs(x - model.xPoly[i]) <= 15 && Math.abs(y - model.yPoly[i]) <= 15) {
    			terrainIndex = i;
    			return true;
    		}
    	}
    	return false;
    }

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
        // Draw World
		Rectangle2D.Double worldBounds = model.getWorldBounds();
		g.setColor(Color.lightGray);
		g.fillRect(0, 0, (int)worldBounds.width, (int)worldBounds.height);
		// Draw Terrain
		g.setColor(Color.darkGray);
		g.fillPolygon(model.terrain);
		// Gray Circles on Terrain
		g.setColor(Color.gray);
		for (int i = 0 ; i < 20 ; ++i){
			g.drawArc(model.xPoly[i] - 15, model.yPoly[i] - 15, 30, 30, 0, 360);
		}
		// Draw Landing Pad
		g.setColor(Color.red);
		g.fillRect(model.padPos.x, model.padPos.y, 40, 10);
	}

    @Override
    public void update(Observable o, Object arg) {
    	repaint();
    }

}
