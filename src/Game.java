import game2D.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import java.awt.image.BufferedImage;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author David Cairns
 */
public class Game extends GameCore implements ActionListener, MouseListener
{
    // width of the screen
    private final int screenWidth = 800;
    // height of the screen
    private final int screenHeight = 600;
    private Sequencer midiSequencer;

    // Game state flags
    private boolean jump = false;
    private boolean left = false;
    private boolean right = false;
    private boolean decelerate = false;
    private boolean canJump = true;
    private boolean touchingGround = false;
    private boolean lastDirectionRight = true; // Tracks last direction (true = right, false = left)
    private boolean attacking = false; // Is the player attacking?

    // Integer values
    private int jumpsDone = 0; // no. jumps performed this jump
    private int levelNumber = 1; // the current level the player is on
    private int coinsCollected = 0; // the number of gems the player has collected
    private int totalCoins = 0; // the total number of gems in the level
    private int lifeRemaining = 3; // the amount of life the player has remaining

    private String portalState = ("Closed"); // the current status of the flag, i.e. can the player finish the level

    // Animations (typically here you would use setScale for the left versions of animations, was unable to get it working)
    private Animation standing_right;

    private Animation standing_left;
    private Animation running_right;
    private Animation running_left;
    private Animation jumping_right;
    private Animation jumping_left;
    private Animation falling_right;
    private Animation falling_left;
    private Animation enemy_running_left;
    private Animation enemy_running_right;
    private Animation attack_right;
    private Animation attack_left;
    private Animation portalAnimLevel1;
    private Animation portalAnimLevel2;


    // Sprites
    private Sprite player = null;
    private Sprite enemy1 = null;
    private Sprite enemy2 = null;
    private Sprite enemy3 = null;
    private Sprite enemy4 = null;
    private Sprite portal = null;

    // The game TileMap
    private final TileMap tmap = new TileMap();    // Our tile map, note that we load it in init()
    public float postX;
    public float postY;

    // Images
    private Image[] parallaxLayers; // Array to store multiple background layers
    private Image heart1; // 3 individual heart images for the player's remaining life
    private Image heart2;
    private Image heart3;

    // Ints representing the positions (in tiles) of the tiles surrounding the player
    int yT; // Tile above the player
    int xT;
    int xB; // Tile below the player
    int yB;
    int xR; // Tile to the right of the player
    int yR;
    int xL; // Tile to the left of the player
    int yL;

    // Unused, just here to stop IntelliJ complaining!
    @Override
    public void actionPerformed(ActionEvent e)
    {

    }

    // Various menu-type screens to improve UX
    public static STATE State = STATE.START;

    // START SCREEN VARIABLES
    private Image logo;
    private float alpha = 0.0f;  // Transparency (0 = transparent, 1 = visible)
    private boolean fadingIn = true;

    /**
     * The obligatory main method that creates
     * an instance of our class and starts it running
     *
     * @param args The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {



        Game game = new Game(); // Create a new instance of Game
        game.init("level1/level1.txt"); // load the first map
        try {
            game.midiSequencer = MidiSystem.getSequencer();
            game.midiSequencer.open();
            Sequence theme = MidiSystem.getSequence(new File("sounds/theme.mid")); // Your MIDI file
            game.midiSequencer.setSequence(theme);
            game.midiSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY); // Loop indefinitely
            game.midiSequencer.start(); // Start playback
            new Sound("sounds/game_start.wav").start(); // This plays immediately after the theme starts
        } catch (MidiUnavailableException | IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }

        int screenWidth = game.screenWidth;
        int screenHeight = game.screenHeight;
        game.run(false, screenWidth, screenHeight);
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it to restart
     * the game.
     */
    public void initialiseGame()
    {

        coinsCollected = 0; // reset variables
        lifeRemaining = 3;

        if (levelNumber == 1) // if on level 1
        {
            // Load the tile map and print it out so we can check it is valid
            tmap.loadMap("maps/level1", "level1.txt");

            totalCoins = 15; // assign number of gems in this level

            player.setX(tmap.getTileXC(3, 6)); // get x & y coordinates of this tile
            player.setY(tmap.getTileYC(3, 6));
            player.setVelocityX(0); // set velocities to 0
            player.setVelocityY(0);

            // Enemy 1
            enemy1.setSpawnX(tmap.getTileXC(11, 4));
            enemy1.setSpawnY(tmap.getTileYC(11, 4));
            enemy1.setMinPatrol(enemy1.getSpawnX() - 15);  // Moves 15 tiles left
            enemy1.setMaxPatrol(enemy1.getSpawnX() + 15);  // Moves 15 tiles right

            // Enemy 2
            enemy2.setSpawnX(tmap.getTileXC(16, 12));
            enemy2.setSpawnY(tmap.getTileYC(16, 12));
            enemy2.setMinPatrol(enemy2.getSpawnX() - 15);  // Moves 20 tiles left
            enemy2.setMaxPatrol(enemy2.getSpawnX() + 15);  // Moves 20 tiles right

            // Enemy 3
            enemy3.setSpawnX(tmap.getTileXC(36, 17));
            enemy3.setSpawnY(tmap.getTileYC(36, 17));
            enemy3.setMinPatrol(enemy3.getSpawnX() - 10);  // Moves 10 tiles left
            enemy3.setMaxPatrol(enemy3.getSpawnX() + 10);  // Moves 10 tiles right

            // Hide Enemy 4
            enemy4.hide();
            enemy4.stop();
            enemy4.setX(-9999); // move it offscreen
            enemy4.setY(-9999);

            // set the spawn points of the red and green flag (start and finish)
            portal.setAnimation(portalAnimLevel1);
            portal.setX(tmap.getTileXC(61, 1));
            portal.setY(tmap.getTileYC(61, 1));
        }
        else if (levelNumber == 2) // if on level 2
        {
            File file = new File("maps/level2/level2.txt");
            if (!file.exists()) {
                System.out.println("ERROR: level2.txt file not found at " + file.getAbsolutePath());
            }
            // similar setup as above

            // Load the tile map and print it out so we can check it is valid
            tmap.loadMap("maps/level2", "level2.txt");

            totalCoins = 30;

            player.setX(tmap.getTileXC(1, 12)); // get x & y coordinates of this tile
            player.setY(tmap.getTileYC(1, 12));
            player.setVelocityX(0); // set velocities to 0
            player.setVelocityY(0);

            // Enemy 1 - Top platform (Row 1, Col 15)
            enemy1.setSpawnX(tmap.getTileXC(5, 12));
            enemy1.setSpawnY(tmap.getTileYC(5, 12));
            enemy1.setMinPatrol(enemy1.getSpawnX() - 10);
            enemy1.setMaxPatrol(enemy1.getSpawnX() + 10);

            // Enemy 2 - Middle platform (Row 4, Col 10)
            enemy2.setSpawnX(tmap.getTileXC(23, 1));
            enemy2.setSpawnY(tmap.getTileYC(23, 1));
            enemy2.setMinPatrol(enemy2.getSpawnX() - 10);
            enemy2.setMaxPatrol(enemy2.getSpawnX() + 10);

            // Enemy 3 - Lower-mid platform (Row 7, Col 5)
            enemy3.setSpawnX(tmap.getTileXC(41, 7));
            enemy3.setSpawnY(tmap.getTileYC(41, 7));
            enemy3.setMinPatrol(enemy3.getSpawnX() - 10);
            enemy3.setMaxPatrol(enemy3.getSpawnX() + 10);

            // Enemy 4 - Bottom platform (Row 13, Col 38)
            enemy4.setSpawnX(tmap.getTileXC(53, 15));
            enemy4.setSpawnY(tmap.getTileYC(53, 15));
            enemy4.setMinPatrol(enemy4.getSpawnX() - 10);
            enemy4.setMaxPatrol(enemy4.getSpawnX() + 10);
            enemy4.show();

            portal.setAnimation(portalAnimLevel2);
            portal.setX(tmap.getTileXC(31, 11));
            portal.setY(tmap.getTileYC(31, 11));
        }
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers
     * @param map The map to be loaded
     */
    public void init(String map)
    {
        logo = new ImageIcon("images/Icons/cyber_forager_logo.png").getImage();

        //background = loadImage("images/background.png").getScaledInstance(728, 455, Image.SCALE_DEFAULT);
        // Load tile map
        tmap.loadMap("maps", map);

        // Load multiple parallax backgrounds and scale to 2x screen width
        parallaxLayers = new Image[]{
                loadAndScaleImage("images/1.png"),
                loadAndScaleImage("images/2.png"),
                loadAndScaleImage("images/3.png"),
                loadAndScaleImage("images/4.png"),
                loadAndScaleImage("images/5.png")
        };

        // === Player Animations ===
        standing_right = loadAnimation("cyborg_standing_right.png", 4, 600);
        standing_left = loadAnimation("cyborg_standing_left.png", 4, 600);
        running_right = loadAnimation("cyborg_run_right.png", 6, 120);
        running_left = loadAnimation("cyborg_run_left.png", 6, 120);
        jumping_right = loadAnimation("cyborg_jump_right.png", 4, 600);
        jumping_left = loadAnimation("cyborg_jump_left.png", 4, 600);
        attack_right = loadAnimation("cyborg_attack_right.png", 8, 100);
        attack_left = loadAnimation("cyborg_attack_left.png", 8, 100);
        falling_right = jumping_right;
        falling_left = jumping_left;

        // === Enemy Animations ===
        enemy_running_left = loadAnimation("anim_enemy_running_left.png", 8, 400);
        enemy_running_right = loadAnimation("anim_enemy_running_right.png", 8, 400);

        // === Portal Animations ===
        portalAnimLevel1 = loadAnimation("portal1.png", 9, 50);
        portalAnimLevel2 = loadAnimation("portal2.png", 8, 50);

        // === Initialize Sprites ===
        player = new Sprite(standing_right);
        enemy1 = new Sprite(enemy_running_right);
        enemy2 = new Sprite(enemy_running_right);
        enemy3 = new Sprite(enemy_running_right);
        enemy4 = new Sprite(enemy_running_right);
        portal = new Sprite(portalAnimLevel1);

        // Initialize game variables
        initialiseGame();

        // Debugging (print the map for verification)
        System.out.println(tmap);

        // Load foreground and health icon
        heart1 = loadImage("images/Heart.png").getScaledInstance(25, 22, Image.SCALE_DEFAULT);
        heart2 = heart1;
        heart3 = heart1;

        addMouseListener(this);
    }

    // === Helper Method to Load Animations ===
    private Animation loadAnimation(String filename, int frames, int frameDuration) {
        Animation anim = new Animation();
        anim.loadAnimationFromSheet("images/Animations/" + filename, frames, 1, frameDuration);
        return anim;
    }

    /**
     * Update any sprites and check for collisions
     *
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */
    public void update(long elapsed)
    {
        if (State == STATE.START) {
            updateStarter();
            return;
        }

        if (State == STATE.GAME)  // If in the game state, i.e. in a level..
        {
            // Add sprites to an array list for easier processing
            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            for (Sprite s : sprites)
            {
                // to control gravity/falling
                float gravity = 0.0010f;
                s.setVelocityY(s.getVelocityY() + (gravity * elapsed)); // readjust velocity due to gravity
            }

            player.setAnimationSpeed(1.0f); // set animation speed for the player
            checkTileCollision(player); // check for further tile collisions

            handlePlayerMovement(elapsed);

            // Add sprites to an array list for easier processing
            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            for (Sprite enemy : enemies)
            {
                if ((enemy.getX() > enemy.getMaxPatrol() && enemy.getDirection()) || (enemy.getX() < enemy.getMinPatrol() && !enemy.getDirection()))  // if the enemy hits the patrol area edge
                {
                    enemy.setDirection(!enemy.getDirection()); // Change the direction boolean
                }
                if (enemy.getDirection()) // if moving right
                {
                    enemy.setVelocityX(0.02f); // move sprite to the right
                    enemy.setAnimation(enemy_running_right); // change animation accordingly
                }
                else // if moving left
                {
                    enemy.setVelocityX(-0.02f); // move sprite to the left
                    enemy.setAnimation(enemy_running_left); // change animation accordingly
                }
                enemy.update(elapsed); // call update method on the enemies //TODO: Maybe unnecessary code?
            }

            // Update animations and positions
            for (Sprite s : sprites)
            {
                s.update(elapsed);
            }

            // Update the portal animation
            portal.update(elapsed);

            // Check for tile map collisions
            for (Sprite s : sprites)
            {
                handleTileMapCollisions(s);
            }

            // Clamp player to screen/map edge
            handleScreenEdge(player, tmap);

            // Check for sprite collisions
            handleSpriteCollisions();

            if (lifeRemaining == 0)
            {
                Sound sound = new Sound("sounds/robot_death.wav"); // load fail/death sound
                sound.start();
//                sound.echo("robot_death.wav");
                System.out.println("You died!"); // inform user that they died
                levelNumber = 1; // reset level number to 1
                resetVariables(); // reset all variables
                Game.State = Game.STATE.DEAD; // Change game state to the 'Dead' state
                if (midiSequencer != null && midiSequencer.isRunning()) {
                    midiSequencer.stop();
                }
            }
        }
    }

    private void handlePlayerMovement(long elapsed) {
        applyGravity(elapsed);

        if (attacking) return; // Don't change animation if attacking

        if (touchingGround) {
            if (lastDirectionRight) {
                player.setAnimation(standing_right); // Face right when idle if last move was right
            } else {
                player.setAnimation(standing_left); // Face left when idle if last move was left
            }
        }

        if (jump && canJump && touchingGround) {
            performJump();
        }

        if (left) {
            moveLeft();
        }
        if (right) {
            moveRight();
        }
        if (decelerate) {
            applyDeceleration();
        }

        updateAirborneAnimation();
    }


    private void applyGravity(long elapsed)
    {
        float gravity = 0.0010f;
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
    }

    private void performJump()
    {
        touchingGround = false;
        if (jumpsDone < 1) {
            if (player.getVelocityY() >= 0) {
                new Sound("sounds/cyborg_jump.wav").start();
                player.setVelocityY(-0.75f);
                player.shiftY(-0.01f);
                jump = false;
                jumpsDone++;
            }
        } else {
            canJump = false;
        }
    }

    private void moveLeft() {
        postX = player.getX() + player.getImage().getWidth(null);
        postY = player.getY() + (float) player.getImage().getHeight(null) / 2;

        if (isWallBlocking(postX - 0.02f, postY)) {
            player.setVelocityX(0);
        } else {
            player.setVelocityX(-0.2f);
            lastDirectionRight = false; // 🔹 Set last direction to left
            if (touchingGround) player.setAnimation(running_left);
        }
    }

    private void moveRight() {
        postX = player.getX() + player.getImage().getWidth(null);
        postY = player.getY() + (float) player.getImage().getHeight(null) / 2;

        if (isWallBlocking(postX + 0.02f, postY)) {
            player.setVelocityX(0);
        } else {
            player.setVelocityX(0.2f);
            lastDirectionRight = true; // 🔹 Set last direction to right
            if (touchingGround) player.setAnimation(running_right);
        }
    }


    private void applyDeceleration()
    {
        player.setVelocityX(player.getVelocityX() * 0.9f);
        if (Math.abs(player.getVelocityX()) <= 0.01f) {
            player.setVelocityX(0);
            decelerate = false;
        }
    }

    private void updateAirborneAnimation()
    {
        if (!touchingGround) {
            if (player.getVelocityY() > 0) {
                player.setAnimation(player.getPlayerDirection() ? falling_right : falling_left);
            } else if (player.getVelocityY() < 0) {
                player.setAnimation(player.getPlayerDirection() ? jumping_right : jumping_left);
            }
        }
    }

    private boolean isWallBlocking(float x, float y)
    {
        char tile = tmap.getTileChar((int)x, (int)y);
        return (tile == 'g' || tile == 'k' || tile == 'q' || tile == 'p' || tile == 'u' || tile == 'o');
    }

    /**
     * Draw the current state of the game
     *
     * @param g the graphics object to draw to
     */
    public void draw(Graphics2D g) {

        if (State == STATE.START) {
            renderStarter(g);
            return;
        }

        // Calculate camera offsets based on player position.
        int xo = (int) -player.getX() + 150;
        int yo = (int) -player.getY() + 300;

        // New parallax drawing using helper method
        drawParallaxLayer(g, parallaxLayers[0], xo, 12);
        drawParallaxLayer(g, parallaxLayers[1], xo, 9);
        drawParallaxLayer(g, parallaxLayers[2], xo, 6);
        drawParallaxLayer(g, parallaxLayers[3], xo, 3);
        drawParallaxLayer(g, parallaxLayers[4], xo, 2); // Optional: add a fifth layer


        if (State == STATE.GAME) {

            // List all sprites to render and check visibility.
            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            for (Sprite s : sprites) {
                s.setOffsets(xo, yo);
                checkOnScreen(g, s, xo, yo);
            }

            // Set offsets and draw the flags.
            portal.setOffsets(xo, yo);
            portal.draw(g);

            // Draw the tile map (main game world).
            tmap.draw(g, xo, yo);

            // Draw score and flag status.
            g.setColor(Color.white);
            String coinMessage = String.format("Coins: %d / %d", coinsCollected, totalCoins);
            g.drawString(coinMessage, getWidth() - 170, 60); // Align score near top-right

            String portalMessage = "Portal: " + portalState;
            g.drawString(portalMessage, (getWidth() / 2) - 90, 60); // Keep portal message centered

            // Draw player hearts (life remaining)
            int heartY = 40;
            int heartSpacing = screenWidth / 30; // ~26px if screenWidth is 800
            int heartX = screenWidth / 40;       // ~20px starting X position

            ArrayList<Image> life = new ArrayList<>();
            life.add(heart1);
            life.add(heart2);
            life.add(heart3);

            for (int i = 0; i < lifeRemaining; i++) {
                g.drawImage(life.get(i), heartX, heartY, null);
                heartX += heartSpacing; // Responsive spacing
            }

            return;
        }

        // Handle special screens like DEAD, HELP, and COMPLETE.
        if (State == STATE.DEAD) {
            g.setColor(Color.BLACK); // Set background to black
            g.fillRect(0, 0, getWidth(), getHeight()); // Fill screen with black

            // === "Game Over" title ===
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String message = "Game Over";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 - 50;
            g.drawString(message, x, y);

            // === "Press R to Restart" aligned below title ===
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            FontMetrics restartFM = g.getFontMetrics();
            String restartMessage = "Press R to Restart";
            int restartX = (getWidth() - restartFM.stringWidth(restartMessage)) / 2;
            int restartY = y + restartFM.getHeight() + 60;
            g.setColor(Color.WHITE);
            g.drawString(restartMessage, restartX, restartY);
        }

        else if (State == STATE.MISSION_SUCCESS) {
            g.setColor(Color.BLACK); // Black background
            g.fillRect(0, 0, getWidth(), getHeight());

            // === "Mission Success!" title ===
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String message = "Mission Success!";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 - 50;
            g.drawString(message, x, y);

            // === "Press R to Restart" aligned below title ===
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            FontMetrics restartFM = g.getFontMetrics();
            String restartMessage = "Press R to Restart";
            int restartX = (getWidth() - restartFM.stringWidth(restartMessage)) / 2;
            int restartY = y + restartFM.getHeight() + 60;
            g.setColor(Color.WHITE);
            g.drawString(restartMessage, restartX, restartY);
        }

    }


    // Method to check if a sprite is on screen

    /**
     *
     * @param g the graphics object to draw to
     * @param s the current sprite
     * @param xo the x offset value
     * @param yo the y offset value
     */
    public void checkOnScreen(Graphics2D g, Sprite s, int xo, int yo)
    {
        Rectangle rect = (Rectangle) g.getClip(); // Create a rectangle around the edges of the screen
        int xc, yc; // variables to register the position of the

        // get the x and y position of the sprite
        xc = (int) (xo + s.getX());
        yc = (int) (yo + s.getY());

        if (rect.contains(xc, yc)) // if the sprite's coordinates are within the rectangle border
        {
            s.show(); // show the sprite
            s.draw(g); // draw them to the screen
        }
        else
        {
            s.hide(); // hide the sprite
        }
    }

    // Method to reset the variables at the end of the game
    public void resetVariables()
    {
        coinsCollected = 0; // reset variables
        lifeRemaining = 3;

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps/level1", "level1.txt");

        totalCoins = 15; // assign number of gems in this level

        player.setX(tmap.getTileXC(3, 6)); // get x & y coordinates of this tile
        player.setY(tmap.getTileYC(3, 6));
        player.setVelocityX(0); // set velocities to 0
        player.setVelocityY(0);

        // Enemy 1
        enemy1.setSpawnX(tmap.getTileXC(11, 4));
        enemy1.setSpawnY(tmap.getTileYC(11, 4));
        enemy1.setMinPatrol(enemy1.getSpawnX() - 15);  // Moves 15 tiles left
        enemy1.setMaxPatrol(enemy1.getSpawnX() + 15);  // Moves 15 tiles right

        // Enemy 2
        enemy2.setSpawnX(tmap.getTileXC(16, 12));
        enemy2.setSpawnY(tmap.getTileYC(16, 12));
        enemy2.setMinPatrol(enemy2.getSpawnX() - 15);  // Moves 20 tiles left
        enemy2.setMaxPatrol(enemy2.getSpawnX() + 15);  // Moves 20 tiles right

        // Enemy 3
        enemy3.setSpawnX(tmap.getTileXC(36, 17));
        enemy3.setSpawnY(tmap.getTileYC(36, 17));
        enemy3.setMinPatrol(enemy3.getSpawnX() - 10);  // Moves 10 tiles left
        enemy3.setMaxPatrol(enemy3.getSpawnX() + 10);  // Moves 10 tiles right

        // Hide Enemy 4
        enemy4.hide();
        enemy4.stop();
        enemy4.setX(-9999); // move it offscreen
        enemy4.setY(-9999);

            // set the spawn points of the red and green flag (start and finish)
            portal.setAnimation(portalAnimLevel1);
            portal.setX(tmap.getTileXC(61, 1));
            portal.setY(tmap.getTileYC(61, 1));
    }

    /**
     * Checks and handles collisions with the tile map for the
     * given sprite 's'. Initial functionality is limited...
     *
     * @param s       The Sprite to check collisions for
     */
    public void handleTileMapCollisions(Sprite s)
    {
        // get the x and y position (in tiles)
        // the x position of the tile (in tiles)
        int tileX = (int) (s.getX() / tmap.getTileWidth());
        //the y position of the tile (in tiles)
        int tileY = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight());

        if (tmap.getTileChar(tileX, tileY) == 'G' || tmap.getTileChar(tileX, tileY) == 'T' ||
                tmap.getTileChar(tileX, tileY) == 'B' || tmap.getTileChar(tileX, tileY) == 'D' ||
                tmap.getTileChar(tileX, tileY) == 'L' || tmap.getTileChar(tileX, tileY) == 'R' ||
                tmap.getTileChar(tileX, tileY) == 'Q' || tmap.getTileChar(tileX, tileY) == 'W' ||
                tmap.getTileChar(tileX, tileY) == ']' || tmap.getTileChar(tileX, tileY) == '\\' ||
                tmap.getTileChar(tileX, tileY) == '/' || tmap.getTileChar(tileX, tileY) == '<' ||
                tmap.getTileChar(tileX, tileY) == '-' || tmap.getTileChar(tileX, tileY) == '>') // If touching ground tile
        {
            if (s.getVelocityY() > 0) // resets Y velocity to prevent falling through the ground
            {
                s.setVelocityY(0);
            }
            s.setY((float) (tileY * tmap.getTileHeight()) - s.getHeight()); // Set the player's Y position to just above the bottom of the tile they're on
            if (s.equals(player)) // reset variables to allow the player to jump again
            {
                jumpsDone = 0;
                touchingGround = true;
            }
        }

        if (tmap.getTileChar(tileX, tileY) == 'V') // If player touches lava //TODO: Player can be forced into a wall after being knocked back - same for enemy damage
        {
            if (s.equals(player))
            {
                Sound damage = new Sound("sounds/cyborg_hurt.wav"); // Load damage sound
                damage.start(); // Run thread
                // decrement remaining life
                if (player.getPlayerDirection()) // if player is moving right
                {
                    player.setVelocityY(-0.2f); // send them flying to the left (recoil damage)
                    player.setVelocityX(-0.2f);
                    player.setX(player.getX() - 10); // set the x position back a little to prevent 3 collisions happening at once
                }
                else  // since player is moving left
                {
                    player.setVelocityY(0.2f); // send them flying to the right
                    player.setVelocityX(0.2f);
                    player.setX(player.getX() + 10);
                }
                player.setY(player.getY() - 10); // set the y position back a little to prevent 3 collisions happening at once
                lifeRemaining--; // decrement remaining life

            } else {
                s.stop();
                s.hide();
            }
        }

        // Code for the collecting of coins
        if ((tmap.getTileChar(tileX, tileY - 1) == '1') && s.equals(player))  // If green gem touched
        {
            Sound collect = new Sound("sounds/coin_collect.wav"); // Load collect sound
            collect.start(); // Run thread
            coinsCollected++; // increment gems collected by the appropriate value
            tmap.setTileChar('.', tileX, tileY - 1); // Replace the gem with an empty space
        }

        if (coinsCollected == totalCoins)
        {
            portalState = ("Open"); // Set portal state to open
            portal.show(); // Show the portal
        }
        if ((tmap.getTileChar(tileX, tileY - 1) == 'H') && s.equals(player)) // If the player touches a heart
        {
            if (lifeRemaining < 3) // If not at full health
            {
                tmap.setTileChar('.', tileX, tileY - 1); // Set the tile the heart was on to a blank space
                lifeRemaining++; // Increment life remaining
            }
        }

    }

    // Method to check tile collision surrounding the player
    /**
     *
     * @param sprite the sprite to check collisions on
     */
    public void checkTileCollision(Sprite sprite)
    {

        // Get position of tile above the player
        xT = (int) ((sprite.getX() / tmap.getTileWidth()) + 0.5);
        yT = (int) (sprite.getY() / tmap.getTileHeight());

        // Get position of tile below the player
        xB = (int) (sprite.getX() / tmap.getTileWidth() + 0.5);
        yB = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 1.0);

        // Get position of tile to the right of the player
        xR = (int) (sprite.getX() / tmap.getTileWidth() + 0.5);
        yR = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        // Get position of tile to the left of the player
        xL = (int) (sprite.getX() / tmap.getTileWidth());
        yL = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        // if the tile above is a 'ground' tile
        if ((tmap.getTileChar(xT, yT) == 'G' || tmap.getTileChar(xT, yT) == 'T' ||
                tmap.getTileChar(xT, yT) == 'B' || tmap.getTileChar(xT, yT) == 'D' ||
                tmap.getTileChar(xT, yT) == 'L' || tmap.getTileChar(xT, yT) == 'R' ||
                tmap.getTileChar(xT, yT) == 'Q' || tmap.getTileChar(xT, yT) == 'W' ||
                tmap.getTileChar(xT, yT) == ']' || tmap.getTileChar(xT, yT) == '\\' ||
                tmap.getTileChar(xT, yT) == '/' || tmap.getTileChar(xT, yT) == '<' ||
                tmap.getTileChar(xT, yT) == '-' || tmap.getTileChar(xT, yT) == '>') &&
                sprite.getVelocityY() > 0) {
            sprite.setVelocityY(0); // stop the player
            canJump = false; // prevents the player from being able to jump when right below a block; and from clipping out of the map
        }
        else
        {
            canJump = true;
        }
        // if the tile to the right is a wall tile
        while ((tmap.getTileChar(xR, yR) == 'G' || tmap.getTileChar(xR, yR) == 'T' ||
                tmap.getTileChar(xR, yR) == 'B' || tmap.getTileChar(xR, yR) == 'D' ||
                tmap.getTileChar(xR, yR) == 'L' || tmap.getTileChar(xR, yR) == 'R' ||
                tmap.getTileChar(xR, yR) == 'Q' || tmap.getTileChar(xR, yR) == 'W' ||
                tmap.getTileChar(xR, yR) == ']' || tmap.getTileChar(xR, yR) == '\\' ||
                tmap.getTileChar(xR, yR) == '/' || tmap.getTileChar(xR, yR) == '<' ||
                tmap.getTileChar(xR, yR) == '-' || tmap.getTileChar(xR, yR) == '>') &&
                sprite.getVelocityX() > 0) {
            sprite.setVelocityX(0); // stop player
            sprite.setX(xR * tmap.getTileWidth() - sprite.getImage().getWidth(null));
        }
        // if the tile to the left is a wall tile
        while ((tmap.getTileChar(xL, yL) == 'G' || tmap.getTileChar(xL, yL) == 'T' ||
                tmap.getTileChar(xL, yL) == 'B' || tmap.getTileChar(xL, yL) == 'D' ||
                tmap.getTileChar(xL, yL) == 'L' || tmap.getTileChar(xL, yL) == 'R' ||
                tmap.getTileChar(xL, yL) == 'Q' || tmap.getTileChar(xL, yL) == 'W' ||
                tmap.getTileChar(xL, yL) == ']' || tmap.getTileChar(xL, yL) == '\\' ||
                tmap.getTileChar(xL, yL) == '/' || tmap.getTileChar(xL, yL) == '<' ||
                tmap.getTileChar(xL, yL) == '-' || tmap.getTileChar(xL, yL) == '>') &&
                sprite.getVelocityX() < 0) {
            sprite.setVelocityX(0); // stop player
            sprite.setX(xL * tmap.getTileWidth() + tmap.getTileWidth());
        }
        // if the tile below is a 'ground' tile
        if (tmap.getTileChar(xB, yB) == 'G' || tmap.getTileChar(xB, yB) == 'T' ||
                tmap.getTileChar(xB, yB) == 'B' || tmap.getTileChar(xB, yB) == 'D' ||
                tmap.getTileChar(xB, yB) == 'L' || tmap.getTileChar(xB, yB) == 'R' ||
                tmap.getTileChar(xB, yB) == 'Q' || tmap.getTileChar(xB, yB) == 'W' ||
                tmap.getTileChar(xB, yB) == ']' || tmap.getTileChar(xB, yB) == '\\' ||
                tmap.getTileChar(xB, yB) == '/' || tmap.getTileChar(xB, yB) == '<' ||
                tmap.getTileChar(xB, yB) == '-' || tmap.getTileChar(xB, yB) == '>') {
            sprite.setVelocityY(0); // stop player
            sprite.shiftY(2); // move them up a little bit
        }
    }

    /**
     * Checks and handles collisions with the edge of the screen. You should generally
     * use tile map collisions to prevent the player leaving the game area. This method
     * is only included as a temporary measure until you have properly developed your
     * tile maps.
     *
     * @param s         The Sprite to check collisions for
     * @param tmap      The tile map to check
     */
    public void handleScreenEdge(Sprite s, TileMap tmap)
    {
        // Bottom edge
        float bottomDifference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (bottomDifference > 0)
        {
            s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(bottomDifference));
            s.setVelocityY(-s.getVelocityY() * 0.75f); // bounce up
        }

        // Left edge
        if (s.getX() < 0)
        {
            s.setX(0);
            s.setVelocityX(0);
        }

        // Right edge
        float rightDifference = s.getX() + s.getWidth() - tmap.getPixelWidth();
        if (rightDifference > 0)
        {
            s.setX(tmap.getPixelWidth() - s.getWidth() - (int)(rightDifference));
            s.setVelocityX(0);
        }
    }

    // Method for handling sprite collisions using a bounding circle
    private void handleSpriteCollisions()
    {

        if (State == STATE.GAME) // Only run if in the 'Game' state
        {
            // Add enemies to ArrayList for easier processing
            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            // (Re)set the collided flag to false
            boolean collided = false;

            for (Sprite enemy : enemies) {
                if (boundingBoxCollision(player, enemy) && BoundingCircleCollision(player, enemy)) {
                    if (attacking) {
                        Sound enemyDeath = new Sound("sounds/enemy_die.wav");
                        enemyDeath.start();
                        enemy.stop();
                        enemy.hide();
                        enemy.setX(0);
                        enemy.setY(0);
                    } else {
                        collided = true;
                    }
                }
            }

            if (collided) {
                Sound damage = new Sound("sounds/cyborg_hurt.wav");
                damage.start();
                if (player.getPlayerDirection()) {
                    player.setVelocityY(-0.2f);
                    player.setVelocityX(-0.2f);
                    player.setX(player.getX() - 10);
                    player.setY(player.getY() - 10);
                } else {
                    player.setVelocityY(0.2f);
                    player.setVelocityX(0.2f);
                    player.setX(player.getX() + 10);
                    player.setY(player.getY() - 10);
                }
                lifeRemaining--;
            }

            if ((BoundingCircleCollision(player, portal)) && portalState.equals("Open"))  // if the player has touched the green flag and enough gems are collected
            {
                Sound levelComplete = new Sound("sounds/level_transition.wav"); // Load level complete sound
                levelComplete.start(); // start thread
                finishLevel(); // call the finishLevel() method
            }
        }
    }

    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     *
     * @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (State == STATE.GAME) {
            if (key == KeyEvent.VK_ESCAPE) stop();
            if (key == KeyEvent.VK_SPACE) jump = true;
            if (key == KeyEvent.VK_LEFT) left = true;
            if (key == KeyEvent.VK_RIGHT) right = true;

            // NEW: Attack Key
            if (key == KeyEvent.VK_A && !attacking) {
                attacking = true;

                new Sound("sounds/cyborg_attack.wav").start();
                if (lastDirectionRight) {
                    player.setAnimation(attack_right);
                } else {
                    player.setAnimation(attack_left);
                }

                // Add a small delay before allowing another attack
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                attacking = false; // Reset attack after animation completes
                            }
                        }, 500 // Adjust based on animation duration (milliseconds)
                );
            }

            if (key == KeyEvent.VK_1) {
                levelNumber = 1;
                init("level1/level1.txt");
                initialiseGame();
            }

            if (key == KeyEvent.VK_2) {
                levelNumber = 2;
                init("level2/level2.txt");
                initialiseGame();
            }

            if (key == KeyEvent.VK_Q) {
                State = STATE.START;
            }
        }

        if (State == STATE.DEAD && key == KeyEvent.VK_R) {
            State = STATE.GAME;
            initialiseGame();  // Restart the game
            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0); // Rewind
                midiSequencer.start(); // Resume
            }
        }

        if (State == STATE.MISSION_SUCCESS && key == KeyEvent.VK_R) {
            levelNumber = 1;
            init("level1/level1.txt");  // Reload level and sprites
            initialiseGame();           // Reset positions and logic
            State = STATE.GAME;
            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0);
                midiSequencer.start(); // Restart background music
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (State == STATE.START && alpha >= 1.0f) {
            System.out.println("Mouse clicked: starting game...");
            State = STATE.GAME;
            initialiseGame();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    public void finishLevel()
    {
        if (levelNumber == 1)  // if on level 1
        {
            levelNumber++; // increment level number
            System.out.println("Level 1 Done!"); // inform user they successfully finished the level
            coinsCollected = 0; // reset the collected gems variable (otherwise the next level will instantly spawn the green flag)
            portalState = "Closed"; // reset flag status
            init("level2/level2.txt"); // load level 2
            initialiseGame(); // re-initialise the game
        }
        else if (levelNumber == 2) {
            System.out.println("Mission Success"); // Player has finished the game

            if (midiSequencer != null && midiSequencer.isRunning()) {
                midiSequencer.stop(); // Stop MIDI background music
            }

            Sound sound = new Sound("win.wav"); // Load win sound
            sound.echo("win.wav"); // Play win sound with echo

            levelNumber = 1; // Reset for next playthrough
            coinsCollected = 0;
            portalState = "Closed";
            Game.State = Game.STATE.MISSION_SUCCESS; // Immediately switch to mission success screen
        }
    }

    /** Use the sample code in the lecture notes to properly detect
     * a bounding box collision between sprites s1 and s2.
     *
     * @return	true if a collision may have occurred, false if it has not.
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
        Rectangle r1 = new Rectangle((int) s1.getX() + 5, (int) s1.getY() + 5, s1.getWidth() - 10, s1.getHeight() - 10);
        Rectangle r2 = new Rectangle((int) s2.getX() + 5, (int) s2.getY() + 5, s2.getWidth() - 10, s2.getHeight() - 10);
        return r1.intersects(r2);
    }

    // method to calculate collision between two sprites

    /**
     *
     * @param one the first sprite to check
     * @param two the second sprite to check
     * @return whether there has been a collision or not
     */
    public boolean BoundingCircleCollision(Sprite one, Sprite two)
    {
        int dx = ((int) one.getX() + one.getWidth() / 2) - ((int) two.getX() + two.getWidth() / 2);
        int dy = ((int) one.getY() + one.getHeight() / 2) - ((int) two.getY() + two.getHeight() / 2);

        // Use a slightly smaller effective radius
        double r1 = one.getWidth() * 0.4;
        double r2 = two.getWidth() * 0.4;

        double distanceSquared = dx * dx + dy * dy;
        double radiusSum = r1 + r2;

        return distanceSquared < radiusSum * radiusSum;
    }

    private void drawParallaxLayer(Graphics2D g, Image img, int xo, int scrollFactor) {
        int layerWidth = img.getWidth(null);
        int layerHeight = img.getHeight(null);
        int drawX = (xo / scrollFactor) % layerWidth;
        if (drawX > 0) drawX -= layerWidth;

        for (int y = 0; y < getHeight(); y += layerHeight) {
            for (int x = drawX; x < getWidth(); x += layerWidth) {
                g.drawImage(img, x, y, null);
            }
        }
    }

    /**
     *
     * @param e the key press event
     */
    public void keyReleased(KeyEvent e)
    {

        int key = e.getKeyCode(); // fetch the key input from the user

        // Using breaks ana switch cases makes the code neater
        switch (key) {
            case KeyEvent.VK_ESCAPE -> stop(); // end the game
            case KeyEvent.VK_SPACE -> {
                jump = false; // set jump flag to false - player no longer currently trying to jump
                canJump = true; // set canJump to true, as the player has taken their finger off the key
            }
            case KeyEvent.VK_LEFT -> {
                left = false; // stop travelling left
                decelerate = true; // start decelerating
            }
            case KeyEvent.VK_RIGHT -> {
                right = false; // stop travelling right
                decelerate = true; // start decelerating
            }
            // Stop attacking immediately
            default -> {
            } // by default (if any other key pressed)
            // break out of statement
        }
    }

    // 🔧 Helper method to load and safely scale images
    private BufferedImage loadAndScaleImage(String path) {
        try {
            Image img = Toolkit.getDefaultToolkit().getImage(path);
            MediaTracker tracker = new MediaTracker(new java.awt.Container());
            tracker.addImage(img, 0);
            tracker.waitForID(0);

            BufferedImage scaled = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, 800, 600, null);
            g2d.dispose();
            return scaled;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateStarter() {
        if (fadingIn && alpha < 1.0f) {
            alpha += 0.01f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fadingIn = false;
            }
        }
    }

    private void renderStarter(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Fill the background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // Fade in logo
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(ac);

        int logoX = (screenWidth - logo.getWidth(null)) / 2;
        int logoY = (screenHeight - logo.getHeight(null)) / 3;
        g2d.drawImage(logo, logoX, logoY, null);

        if (alpha >= 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String message = "Click Anywhere to Start";
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(message);
            int textX = (screenWidth - msgWidth) / 2;
            int textY = logoY + logo.getHeight(null) + 50;
            g2d.drawString(message, textX, textY);
        }
    }

    public enum STATE // an enumerated list of states for the game to use (as above)
    {
        START,
        GAME,
        DEAD,
        MISSION_SUCCESS
    }
}