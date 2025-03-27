import javax.swing.*;
import java.awt.*;

public class Starter extends JPanel {

    private Image logo;
    private float alpha = 0.0f;  // Transparency level (0 = fully transparent, 1 = fully visible)
    private boolean fadingIn = true;

    public Starter() {
        // Load logo image (ensure the path is correct)
        ImageIcon logoIcon = new ImageIcon("images/Icons/cyber_forager_logo.png");
        logo = logoIcon.getImage();
    }

    public void render(Graphics g) {
        Game game = new Game(); // Create a new instance of Game
        Graphics2D g2d = (Graphics2D) g;

        // Get the panel size dynamically
        int width = game.getScreenWidth();
        int height = game.getScreenHeight();

        // Fill the background dynamically
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // Set transparency for fading effect
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(ac);

        // Center the logo
        int logoX = (width - logo.getWidth(null)) / 2;
        int logoY = (height - logo.getHeight(null)) / 3;
        g2d.drawImage(logo, logoX, logoY, null);

        // Show "Press SPACE to Start" when fully faded in
        if (alpha >= 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));

            String message = "Press SPACE to Start";
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(message);
            int textX = (width - msgWidth) / 2;
            int textY = logoY + logo.getHeight(null) + 50; // Adjust text spacing

            g2d.drawString(message, textX, textY);
        }
    }

    public void update() {
        if (fadingIn && alpha < 1.0f) {
            alpha += 0.01f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fadingIn = false;
            }
        }
    }

    public boolean isFadedIn() {
        return alpha >= 1.0f;
    }
}
