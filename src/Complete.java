import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Complete implements MouseListener // Game finished screen
{

    // Images to use in the menu
    Image complete_icon;
    Image quit_button;
    Image return_button;

    public void render(Graphics g) // Method to draw items to the menu
    {
        Graphics2D g3 = (Graphics2D) g; // Graphics object to draw to

        // Draw images to the screen
        ImageIcon complete_i = new ImageIcon("images/Icons/icon_game_complete.png"); // create new ImageIcon
        complete_icon = complete_i.getImage(); // fetch image
        g.drawImage(complete_icon, 70, 50, null); // draw to screen

        ImageIcon return_b = new ImageIcon("images/Icons/icon_return.png");
        return_button = return_b.getImage();
        g.drawImage(return_button, 160, 200, null);

        ImageIcon quit_b = new ImageIcon("images/Icons/icon_quit.png");
        quit_button = quit_b.getImage();
        g.drawImage(quit_button, 160, 250, null);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) // Handlers for clicking on the menu items
    {
        // Coordinates of the mouse pointer
        int mx = e.getX();
        int my = e.getY();

        if (mx >= 160 && mx <= 340) // If within this horizontal area
        {
            if (my >= 200 && my<= 240) // If within this vertical area
            {
                //Pressed Return
                Game.State = Game.STATE.MENU; // Change to the 'Menu' state
            }
            if (my >= 250 && my <= 290)
            {
                //Pressed Quit
                System.exit(0); // Exit the program
            }
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

}
