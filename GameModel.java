import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import javax.swing.undo.*;
import javax.vecmath.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.undo.*;


public class GameModel extends Observable {

    // Constructor
    public GameModel(int fps, int width, int height, int peaks) {

        undoManager = new UndoManager();

        // landing pad
        padPos = new Point(330, 100);
        pad = new Rectangle2D.Double( 330, 100, 40, 10);

        // ship
        ship = new Ship(60, width/2, 50);
        shipBox = new Rectangle2D.Double( (width/2) - 5 , 50 - 5, 10, 10);
        crashed = false;
        landed = false;

        // world
        worldBounds = new Rectangle2D.Double(0, 0, width, height);

        // build terrain
        Random rand = new Random(); // make random number generator for heights

        xPoly = new int[22];  // x values
        yPoly = new int[22];  // y values

        // Evenly space x coordinates, randomize y coordinates
        for (int i = 0 ; i < 20 ; ++i) {
            xPoly[i] = (i * width) / 19;
            yPoly[i] = rand.nextInt(100) + 100;
        }

        // Bottom right corner, and bottom left corner
        xPoly[20] = width; yPoly[20] = height;
        xPoly[21] = 0; yPoly[21] = height;

        terrain = new Polygon(xPoly, yPoly, 22);

        // anonymous class to monitor ship updates
        ship.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if ( !crashed && !landed ) moveShip();
                setChangedAndNotify();
            }
        });
    } // Constructor

    // Undo manager
    private UndoManager undoManager;

    // Landing Pad
    // - - - - - - - - - - -

    public Point padPos;    // indicates the top left corner of the pad

    public Rectangle2D.Double pad;

    // difference between x y of mouse pos'n and x y of item being dragged/moved
    private int xdiff;
    private int ydiff;

    // initial pad position - used for undo manager
    private int startX;
    private int startY;

    // Used at start of click & drag of landing pad
    public void startPadMove(int x, int y) {
        xdiff = x - padPos.x;
        ydiff = y - padPos.y;
        startX = padPos.x;
        startY = padPos.y;
    }

    // Used for dragging landing pad
    public void padDrag(int x, int y) {
        int newX = x - xdiff;
        int newY = y - ydiff;
        if ( padLimitsBreach((double)newX, (double)newY) )
            return;
        padPos.x = newX;
        padPos.y = newY;
        pad = new Rectangle2D.Double( padPos.x, padPos.y, 40, 10);
        setChangedAndNotify();
    }

    // Done click & drag
    public void finishPadMove() {
        padBackup(padPos.x, padPos.y);
        setChangedAndNotify();
    }

    // Used by double-click to move pad
    public void movePad(MouseEvent e) {
        startX = padPos.x;
        startY = padPos.y;
        int newX = e.getX() - 20;   // 20 is PADWIDTH / 2
        int newY = e.getY() - 5;    // 5 is PADHEIGHT / 2
        if ( padLimitsBreach((double)newX, (double)newY)) {
            padBackup(padPos.x, padPos.y);
            return;
        }
        padBackup(newX, newY);      // add change to undo manager
        padPos.x = newX;
        padPos.y = newY;
        pad = new Rectangle2D.Double( padPos.x, padPos.y, 40, 10);
        setChangedAndNotify();
    }

    // adds this move to the undoManager
    private void padBackup(int x, int y) {

        System.out.println( "backing up");

        // create undoable edit
        UndoableEdit undoableEdit = new AbstractUndoableEdit() {

            // capture variables for closure
            final int oldX = startX;
            final int oldY = startY;
            final int newX = x;
            final int newY = y;

            // Method that is called when we must redo the undone action
            public void redo() throws CannotRedoException {
                super.redo();
                padPos = new Point(newX, newY);
                pad = new Rectangle2D.Double( padPos.x, padPos.y, 40, 10);
                setChangedAndNotify();
            }

            public void undo() throws CannotUndoException {
                super.undo();
                System.out.println("undoing");
                padPos = new Point(oldX, oldY);
                pad = new Rectangle2D.Double( padPos.x, padPos.y, 40, 10);
                setChangedAndNotify();
            }
        };

        // Add this undoable edit to the undo manager
        undoManager.addEdit(undoableEdit);
    } // padBackup

    // Pad Limits Breach (boolean check)
    // if new pad position breaches world boundary, returns true and places the pad on the boundary
    // returns false otherwise
    private boolean padLimitsBreach(double x, double y) {
        if ( worldBounds.contains(x, y) &&
             worldBounds.contains(x + 40, y) &&
             worldBounds.contains(x, y + 10) &&
             worldBounds.contains(x + 40, y + 10))
            return false;
        // this code executes if some part is beyond the edge
        if (x < 0) {
            padPos.x = 0;
        } else if ( x + 40 > worldBounds.width) {
            padPos.x = (int)worldBounds.width - 40;
        } else {
            padPos.x = (int)x;
        }
        if (y < 0) {
            padPos.y = 0;
        } else if ( y + 10 > worldBounds.height) {
            padPos.y = (int)worldBounds.height - 10;
        } else {
            padPos.y = (int)y;
        }
        pad = new Rectangle2D.Double( padPos.x, padPos.y, 40, 10);
        setChangedAndNotify();
        return true;
    }

    // World
    // - - - - - - - - - - -

    public final Rectangle2D.Double getWorldBounds() {
        return worldBounds;
    }

    Rectangle2D.Double worldBounds;

    // Terrain
    public int xPoly[];
    public int yPoly[];
    public Polygon terrain;
    private int terrainIndex;  // which piece of terrain to move
    private int terrainMoveStart; // starting position, used for undo and redo edits

    public void startTerrainMove(int y, int index) {
        terrainIndex = index;
        ydiff = y - yPoly[terrainIndex];
        terrainMoveStart = yPoly[terrainIndex];
    }

    public void terrainMove(int y) {
        int newY = y - ydiff;
        // Check for world breach
        if (newY < 0) {
            newY = 0;
        } else if (newY > worldBounds.height) {
            newY = (int)worldBounds.height;
        }
        // Set new location & update
        yPoly[terrainIndex] = newY;
        terrain = new Polygon(xPoly, yPoly, 22);
        setChangedAndNotify();
    }

    public void finishTerrainMove() {
        // create undoable edit
        UndoableEdit undoableEdit = new AbstractUndoableEdit() {

            // capture variables for closure
            final int index = terrainIndex;
            final int oldValue = terrainMoveStart;
            final int newValue = yPoly[terrainIndex];

            // Method that is called when we must redo the undone action
            public void redo() throws CannotRedoException {
                super.redo();
                yPoly[index] = newValue;
                terrain = new Polygon(xPoly, yPoly, 22);
                setChangedAndNotify();
            }

            public void undo() throws CannotUndoException {
                super.undo();
                yPoly[index] = oldValue;
                terrain = new Polygon(xPoly, yPoly, 22);
                setChangedAndNotify();
            }
        };

        // Add this undoable edit to the undo manager
        undoManager.addEdit(undoableEdit);
        setChangedAndNotify();
        System.out.println("Done terrain move");
    }

    // Ship
    // - - - - - - - - - - -

    public Ship ship;
    public boolean crashed;
    public boolean landed;

    Rectangle2D.Double shipBox;

    // called by Observer for ship, updates the rectangle and checks for landing/crash
    private void moveShip() {
        shipBox = new Rectangle2D.Double(ship.getPosition().getX() - 5, ship.getPosition().getY() - 5, 10, 10);
        if (terrain.intersects(shipBox)) {
            crashed = true;
            ship.setPaused(true);
        } else if (!worldBounds.contains(shipBox)) {
            crashed = true;
            ship.setPaused(true);
        } else if (pad.intersects(shipBox)) {
            if (ship.getSpeed() < ship.getSafeLandingSpeed())
                landed = true;
            else
                crashed = true;
            ship.setPaused(true);
        }
    }

    // helper function to do both
    void setChangedAndNotify() {
        setChanged();
        notifyObservers();
    }

    // undo and redo methods
    // - - - - - - - - - - - - - -

    public void undo() {
        if (canUndo())
            undoManager.undo();
    }

    public void redo() {
        if (canRedo())
            undoManager.redo();
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }


}



