import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Help implements MouseListener // Help screen
{

    // Images to use in menu
    Image return_button;
    Image game_instructions;

    public void render(Graphics g) // Method to draw items to the menu
    {
        Graphics2D g4 = (Graphics2D) g; // Graphics object to draw to

        // Draw images to the screen
        ImageIcon instructions_icon = new ImageIcon("images/Icons/icon_instructions.png"); // create new ImageIcon
        game_instructions = instructions_icon.getImage(); // fetch image
        g.drawImage(game_instructions, 140, 50, null); // draw to screen

        ImageIcon return_b = new ImageIcon("images/Icons/icon_return.png");
        return_button = return_b.getImage();
        g.drawImage(return_button, 160, 300, null);
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
            if (my >= 300 && my<= 340) // If within this vertical area
            {
                //Pressed Return
                Game.State = Game.STATE.MENU; // Change to the 'Menu' state
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
