import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;
import java.text.DecimalFormat;

public class MessageView extends JPanel implements Observer {

    // status messages for game
    JLabel fuel = new JLabel("Fuel: 50");
    JLabel speed = new JLabel("Speed: 0.00");
    JLabel message = new JLabel("(Paused)");

    GameModel model;

    DecimalFormat ff;   // fuel format for display
    DecimalFormat sf;   // speed format for display

    // Constructor
    public MessageView(GameModel model) {

        this.model = model;
        model.addObserver(this);
        ff = new DecimalFormat("#.##");
        sf = new DecimalFormat("##");

        // want the background to be black
        setBackground(Color.BLACK);

        setLayout(new FlowLayout(FlowLayout.LEFT));

        add(fuel);
        add(speed);
        add(message);

        for (Component c: this.getComponents()) {
            c.setForeground(Color.WHITE);
            c.setPreferredSize(new Dimension(100, 20));
        }
    } // Constructor


    @Override
    public void update(Observable o, Object arg) {
        // fuel
        double fuelAmt = model.ship.getFuel();
        if (fuelAmt < 10)
            fuel.setForeground(Color.RED);
        else 
            fuel.setForeground(Color.WHITE);
        fuel.setText("Fuel: " + sf.format(fuelAmt));
        // speed
        double speedAmt = model.ship.getSpeed();
        if (speedAmt < model.ship.getSafeLandingSpeed())
            speed.setForeground(Color.GREEN);
        else
            speed.setForeground(Color.WHITE);
        speed.setText("Speed: " + ff.format(speedAmt));
        // message
        if (model.crashed)
            message.setText("CRASH");
        else if (model.landed)
            message.setText("LANDED!");
        else if (model.ship.isPaused())
            message.setText("(Paused)");
        else
            message.setText("");
    }
}