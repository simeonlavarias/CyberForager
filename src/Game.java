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

public class Game extends GameCore implements ActionListener, MouseListener
{
    // === Screen Configuration ===
    private final int screenWidth = 800;   // Width of the game window
    private final int screenHeight = 600;  // Height of the game window

    // === Audio ===
    private Sequencer midiSequencer;       // Background music sequencer (MIDI player)

    // === Player Control Flags ===
    private boolean jump = false;          // Indicates if jump key is pressed
    private boolean left = false;          // Indicates if left movement is active
    private boolean right = false;         // Indicates if right movement is active
    private boolean decelerate = false;    // Indicates if player should decelerate
    private boolean canJump = true;        // Determines if player is allowed to jump
    private boolean touchingGround = false;// True when player is grounded
    private boolean lastDirectionRight = true; // Tracks the last direction faced (true = right)
    private boolean attacking = false;     // True if player is in attack animation

    // === Fade-in Effect (Start Screen) ===
    private boolean fadingIn = true;       // Controls logo fade-in effect
    private float alpha = 0.0f;            // Transparency level of logo (0.0 = invisible, 1.0 = fully visible)

    // === Game State Timing ===
    private long loadingStartTime = 0;     // Used to measure loading screen duration

    // === Game Progress Tracking ===
    private int jumpsDone = 0;             // Number of jumps completed in current jump session
    private int levelNumber = 1;           // Current level number
    private int coinsCollected = 0;        // Total coins collected by the player
    private int totalCoins = 0;            // Total number of coins available in the level
    private int lifeRemaining = 3;         // Player's remaining lives

    // === Portal ===
    private String portalState = "Closed"; // Current portal state (Closed/Open)

    // === Animations ===
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

    // === Sprites ===
    private Sprite player = null;
    private Sprite enemy1 = null;
    private Sprite enemy2 = null;
    private Sprite enemy3 = null;
    private Sprite enemy4 = null;
    private Sprite portal = null;

    // === Tile Map ===
    private final TileMap tmap = new TileMap();  // The game's tile map

    // Used for collision detection when moving left/right
    public float postX;
    public float postY;

    // === Images ===
    private Image[] parallaxLayersLevel1;  // Parallax backgrounds for level 1
    private Image[] parallaxLayersLevel2;  // Parallax backgrounds for level 2
    private Image heart1;                  // Heart icon for 1 life
    private Image heart2;                  // Heart icon for 2 lives
    private Image heart3;                  // Heart icon for 3 lives
    private Image logo;                    // Game logo for the start screen

    // === Tile Coordinates for Collision Checks ===
    int yT, xT;  // Tile above player
    int xB, yB;  // Tile below player
    int xR, yR;  // Tile to the right of player
    int xL, yL;  // Tile to the left of player

    // === Game State ===
    public static STATE State = STATE.START;  // Initial game state

    // === Entry Point ===

    /**
     * The entry point of the game.
     * Initializes the game, loads the level, starts background music,
     * and launches the game loop.
     */
    public static void main(String[] args) {
        // Create a new instance of the Game class
        Game game = new Game();

        // Load the first level map
        game.init("level1.txt");

        try {
            // Initialize and start the MIDI background music
            game.midiSequencer = MidiSystem.getSequencer();
            game.midiSequencer.open();
            Sequence theme = MidiSystem.getSequence(new File("sounds/theme.mid"));
            game.midiSequencer.setSequence(theme);
            game.midiSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY); // Loop music
            game.midiSequencer.start();

            // Play startup sound effect
            new Sound("sounds/game_start.wav").start();

        } catch (MidiUnavailableException | IOException | InvalidMidiDataException e) {
            e.printStackTrace(); // Print any errors if loading music fails
        }

        // Start the game loop with defined screen size
        int screenWidth = game.screenWidth;
        int screenHeight = game.screenHeight;
        game.run(false, screenWidth, screenHeight);
    }

    /**
     * Initializes the game: loads the level, background layers,
     * animations, sprites, and other assets. Also sets up listeners.
     *
     * @param map The name of the map file to load
     */
    public void init(String map) {
        // === Load Start Screen Logo ===
        logo = new ImageIcon("images/Logo/cyber_forager_logo.png").getImage();

        // === Load the Tile Map based on the level number ===
        if (levelNumber == 1) {
            tmap.loadMap("maps/level1", map);
        } else if (levelNumber == 2) {
            tmap.loadMap("maps/level2", map);
        }

        System.out.println("🗺️ [Game] Current levelNumber = " + levelNumber);

        // === Load Parallax Background Layers ===
        parallaxLayersLevel1 = new Image[]{
                loadAndScaleImage("images/level1_bg/1.png"),
                loadAndScaleImage("images/level1_bg/2.png"),
                loadAndScaleImage("images/level1_bg/3.png"),
                loadAndScaleImage("images/level1_bg/4.png"),
                loadAndScaleImage("images/level1_bg/5.png")
        };

        parallaxLayersLevel2 = new Image[]{
                loadAndScaleImage("images/level2_bg/1.png"),
                loadAndScaleImage("images/level2_bg/2.png"),
                loadAndScaleImage("images/level2_bg/3.png"),
                loadAndScaleImage("images/level2_bg/4.png"),
                loadAndScaleImage("images/level2_bg/5.png")
        };

        // === Load Player Animations ===
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

        // === Load Enemy Animations ===
        enemy_running_left = loadAnimation("anim_enemy_running_left.png", 4, 400);
        enemy_running_right = loadAnimation("anim_enemy_running_right.png", 4, 400);

        // === Load Portal Animations ===
        portalAnimLevel1 = loadAnimation("portal1.png", 9, 50);
        portalAnimLevel2 = loadAnimation("portal2.png", 8, 50);

        // === Create Sprites ===
        player = new Sprite(standing_right);
        enemy1 = new Sprite(enemy_running_right);
        enemy2 = new Sprite(enemy_running_right);
        enemy3 = new Sprite(enemy_running_right);
        enemy4 = new Sprite(enemy_running_right);
        portal = new Sprite(portalAnimLevel1);

        // === Initialize Game State (positions, enemies, etc.) ===
        initialiseGame();

        // Print tile map to console for debugging
        System.out.println(tmap);

        // === Load UI Assets (Hearts) ===
        heart1 = loadImage("images/Heart.png").getScaledInstance(25, 22, Image.SCALE_DEFAULT);
        heart2 = heart1;
        heart3 = heart1;

        // === Register Mouse Listener ===
        addMouseListener(this);
    }

    /**
     * Sets up the initial state of the game for the current level.
     * Resets player position, enemy positions and patrol areas, portal status,
     * and collected items. Handles logic for both Level 1 and Level 2.
     */
    public void initialiseGame() {

        // === Reset game progress ===
        coinsCollected = 0;
        lifeRemaining = 3;

        // === Level 1 Setup ===
        if (levelNumber == 1) {
            totalCoins = 15;  // Total number of coins in Level 1

            // Set player spawn position
            player.setX(tmap.getTileXC(3, 6));
            player.setY(tmap.getTileYC(3, 6));
            player.setVelocityX(0);
            player.setVelocityY(0);

            // === Enemy Setup for Level 1 ===
            enemy1.setSpawnX(tmap.getTileXC(12, 5));
            enemy1.setSpawnY(tmap.getTileYC(12, 5));
            enemy1.setMinPatrol(enemy1.getSpawnX() - 15);
            enemy1.setMaxPatrol(enemy1.getSpawnX() + 15);

            enemy2.setSpawnX(tmap.getTileXC(16, 12));
            enemy2.setSpawnY(tmap.getTileYC(16, 12));
            enemy2.setMinPatrol(enemy2.getSpawnX() - 15);
            enemy2.setMaxPatrol(enemy2.getSpawnX() + 15);

            enemy3.setSpawnX(tmap.getTileXC(36, 11));
            enemy3.setSpawnY(tmap.getTileYC(36, 11));
            enemy3.setMinPatrol(enemy3.getSpawnX() - 8);
            enemy3.setMaxPatrol(enemy3.getSpawnX() + 8);

            // Enemy 4 is not used in Level 1
            enemy4.hide();
            enemy4.stop();
            enemy4.setX(-9999);  // Move off-screen
            enemy4.setY(-9999);

            // Setup portal for Level 1
            portal.setAnimation(portalAnimLevel1);
            portal.setX(tmap.getTileXC(61, 3));
            portal.setY(tmap.getTileYC(61, 3));
        }

        // === Level 2 Setup ===
        else if (levelNumber == 2) {
            // Debug: Check if level2.txt exists
            File file = new File("maps/level2/level2.txt");
            if (!file.exists()) {
                System.out.println("ERROR: level2.txt file not found at " + file.getAbsolutePath());
            }

            totalCoins = 30;  // Total coins in Level 2

            // Set player spawn position
            player.setX(tmap.getTileXC(1, 10));
            player.setY(tmap.getTileYC(1, 10));
            player.setVelocityX(0);
            player.setVelocityY(0);

            // === Enemy Setup for Level 2 ===
            enemy1.setSpawnX(tmap.getTileXC(5, 12));
            enemy1.setSpawnY(tmap.getTileYC(5, 12));
            enemy1.setMinPatrol(enemy1.getSpawnX() - 10);
            enemy1.setMaxPatrol(enemy1.getSpawnX() + 10);

            enemy2.setSpawnX(tmap.getTileXC(23, 0));
            enemy2.setSpawnY(tmap.getTileYC(23, 0));
            enemy2.setMinPatrol(enemy2.getSpawnX() - 10);
            enemy2.setMaxPatrol(enemy2.getSpawnX() + 10);

            enemy3.setSpawnX(tmap.getTileXC(33, 6));
            enemy3.setSpawnY(tmap.getTileYC(33, 6));
            enemy3.setMinPatrol(enemy3.getSpawnX() - 10);
            enemy3.setMaxPatrol(enemy3.getSpawnX() + 10);

            enemy4.setSpawnX(tmap.getTileXC(53, 15));
            enemy4.setSpawnY(tmap.getTileYC(53, 15));
            enemy4.setMinPatrol(enemy4.getSpawnX() - 10);
            enemy4.setMaxPatrol(enemy4.getSpawnX() + 10);
            enemy4.setX(enemy4.getSpawnX());
            enemy4.setY(enemy4.getSpawnY());
            enemy4.show();

            // Setup portal for Level 2
            portal.setAnimation(portalAnimLevel2);
            portal.setX(tmap.getTileXC(31, 11));
            portal.setY(tmap.getTileYC(31, 11));
        }

        // === Reset Enemy States ===
        resetEnemies();
    }

    /**
     * Resets the game state after death or full restart.
     * Loads Level 1 and reinitializes player and enemy positions,
     * as well as collectibles and portal.
     */
    public void resetGame() {
        // === Reset Game Progress ===
        coinsCollected = 0;
        lifeRemaining = 3;

        // === Reload Level 1 Map ===
        tmap.loadMap("maps/level1", "level1.txt");
        totalCoins = 15;

        // === Reset Player Position and Velocity ===
        player.setX(tmap.getTileXC(3, 6));
        player.setY(tmap.getTileYC(3, 6));
        player.setVelocityX(0);
        player.setVelocityY(0);

        // === Enemy Setup (Same as Level 1) ===
        enemy1.setSpawnX(tmap.getTileXC(12, 5));
        enemy1.setSpawnY(tmap.getTileYC(12, 5));
        enemy1.setMinPatrol(enemy1.getSpawnX() - 15);
        enemy1.setMaxPatrol(enemy1.getSpawnX() + 15);

        enemy2.setSpawnX(tmap.getTileXC(16, 12));
        enemy2.setSpawnY(tmap.getTileYC(16, 12));
        enemy2.setMinPatrol(enemy2.getSpawnX() - 15);
        enemy2.setMaxPatrol(enemy2.getSpawnX() + 15);

        enemy3.setSpawnX(tmap.getTileXC(36, 11));
        enemy3.setSpawnY(tmap.getTileYC(36, 11));
        enemy3.setMinPatrol(enemy3.getSpawnX() - 8);
        enemy3.setMaxPatrol(enemy3.getSpawnX() + 8);

        // Enemy 4 is hidden and disabled in Level 1
        enemy4.hide();
        enemy4.stop();
        enemy4.setX(-9999); // Move off-screen
        enemy4.setY(-9999);

        // === Reset Portal ===
        portal.setAnimation(portalAnimLevel1);
        portal.setX(tmap.getTileXC(61, 1));
        portal.setY(tmap.getTileYC(61, 1));

        // === Reset Enemy States ===
        resetEnemies();
    }

    /**
     * Handles level completion logic.
     * If Level 1 is completed, transition to Level 2.
     * If Level 2 is completed, stop the music, play the win sound,
     * and switch to the mission success screen.
     */
    public void finishLevel() {

        // === Transition from Level 1 to Level 2 ===
        if (levelNumber == 1) {
            levelNumber = 2;                 // Advance to next level
            System.out.println("Level 1 Done!");
            coinsCollected = 0;             // Reset collected coins
            portalState = "Closed";         // Reset portal
            System.out.println("🚀 [Game] Transitioning to Level 2...");

            init("level2.txt");             // Load level 2 map
            initialiseGame();               // Re-initialize game state for Level 2
        }

        // === End of Game: Level 2 Completed ===
        else if (levelNumber == 2) {
            System.out.println("Mission Success");

            // Stop background music if still playing
            if (midiSequencer != null && midiSequencer.isRunning()) {
                midiSequencer.stop();
            }

            // Play win sound with echo effect
            Sound sound = new Sound("win.wav");
            sound.echo("win.wav");

            // Reset state for replay
            levelNumber = 1;
            coinsCollected = 0;
            portalState = "Closed";

            // Switch to mission success screen
            Game.State = Game.STATE.MISSION_SUCCESS;
        }
    }

    // === Game Loop ===

    /**
     * Main game loop update method.
     * Updates game logic depending on the current state:
     * START, LOADING, or GAME.
     *
     * @param elapsed Time elapsed since last update call (in milliseconds)
     */
    public void update(long elapsed) {

        // === Handle Start Screen State ===
        if (State == STATE.START) {
            updateStarter();  // Update fade-in effect
            return;           // Skip the rest of the game logic
        }

        // === Handle Loading Screen State ===
        if (State == STATE.LOADING) {
            int loadingDuration = 5000;  // 5 seconds
            if (System.currentTimeMillis() - loadingStartTime >= loadingDuration) {
                State = STATE.GAME;      // Transition to game
                initialiseGame();        // Setup the level
            }
            return;
        }

        // === Main Game Logic (During Gameplay) ===
        if (State == STATE.GAME) {

            // === Prepare Sprite List (Player + Enemies) ===
            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            // === Apply Gravity to All Sprites ===
            for (Sprite s : sprites) {
                float gravity = 0.0010f;
                s.setVelocityY(s.getVelocityY() + (gravity * elapsed));
            }

            // === Player Movement & Collision ===
            player.setAnimationSpeed(1.0f);
            checkTileCollision(player);
            handlePlayerMovement(elapsed);

            // === Handle Enemy Patrol Movement ===
            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            for (Sprite enemy : enemies) {
                // Reverse direction when reaching patrol bounds
                if ((enemy.getX() > enemy.getMaxPatrol() && enemy.getDirection()) ||
                        (enemy.getX() < enemy.getMinPatrol() && !enemy.getDirection())) {
                    enemy.setDirection(!enemy.getDirection());
                }

                // Apply velocity and animation based on direction
                if (enemy.getDirection()) {
                    enemy.setVelocityX(0.02f);
                    enemy.setAnimation(enemy_running_right);
                } else {
                    enemy.setVelocityX(-0.02f);
                    enemy.setAnimation(enemy_running_left);
                }

                enemy.update(elapsed); // Update enemy movement
            }

            // === Update All Sprite Animations ===
            for (Sprite s : sprites) {
                s.update(elapsed);
            }

            // === Update Portal Animation ===
            portal.update(elapsed);

            // === Check Collisions for All Sprites ===
            for (Sprite s : sprites) {
                handleTileMapCollisions(s);
            }

            // === Keep Player Within Bounds of Map ===
            handleScreenEdge(player, tmap);

            // === Handle Collisions Between Player and Enemies/Portal ===
            handleSpriteCollisions();

            // === Check Player Life ===
            if (lifeRemaining == 0) {
                // Play death sound and reset game
                Sound sound = new Sound("sounds/robot_death.wav");
                sound.start();
                System.out.println("You died!");

                levelNumber = 1;
                resetGame();
                Game.State = Game.STATE.DEAD;

                // Stop background music
                if (midiSequencer != null && midiSequencer.isRunning()) {
                    midiSequencer.stop();
                }
            }
        }
    }

    /**
     * Renders the game depending on the current state:
     * START, LOADING, GAME, DEAD, or MISSION_SUCCESS.
     *
     * @param g The graphics context to draw onto
     */
    public void draw(Graphics2D g) {

        // === Start Screen ===
        if (State == STATE.START) {
            renderStarter(g);
            return;
        }

        // === Set Camera Offset Based on Player Position ===
        int xo = (int) -player.getX() + 150;
        int yo = (int) -player.getY() + 300;

        // === Select Background Based on Level ===
        Image[] currentBackground = (levelNumber == 1) ? parallaxLayersLevel1 : parallaxLayersLevel2;

        // === Draw Parallax Background Layers ===
        drawParallaxLayer(g, currentBackground[0], xo, 12);
        drawParallaxLayer(g, currentBackground[1], xo, 9);
        drawParallaxLayer(g, currentBackground[2], xo, 6);
        drawParallaxLayer(g, currentBackground[3], xo, 3);
        drawParallaxLayer(g, currentBackground[4], xo, 2);

        // === Loading Screen ===
        if (State == STATE.LOADING) {
            renderLoading(g);
            return;
        }

        // === Main Game Rendering ===
        if (State == STATE.GAME) {

            // Collect sprites for rendering
            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            // === Draw Tile Map and Decorative Tiles ===
            tmap.draw(g, xo, yo);
            for (TileMap.DecorativeTile dt : tmap.getDecorativeTiles()) {
                g.drawImage(dt.image, dt.x + xo, dt.y + yo, null);
            }

            // === Draw Sprites ===
            for (Sprite s : sprites) {
                s.setOffsets(xo, yo);            // Apply camera offset
                checkSpriteVisible(g, s, xo, yo);     // Only draw if visible
            }

            // === Draw Portal ===
            portal.setOffsets(xo, yo);
            portal.draw(g);

            // === UI: Coins Collected and Portal Status ===
            g.setColor(Color.white);
            String coinMessage = String.format("Coins: %d / %d", coinsCollected, totalCoins);
            g.drawString(coinMessage, getWidth() - 170, 60);  // Top-right

            String portalMessage = "Portal: " + portalState;
            g.drawString(portalMessage, (getWidth() / 2) - 90, 60);  // Centered

            // === UI: Life/Hearts ===
            int heartY = 40;
            int heartSpacing = screenWidth / 30;  // Evenly spaced
            int heartX = screenWidth / 40;        // Left side

            ArrayList<Image> life = new ArrayList<>();
            life.add(heart1);
            life.add(heart2);
            life.add(heart3);

            for (int i = 0; i < lifeRemaining; i++) {
                g.drawImage(life.get(i), heartX, heartY, null);
                heartX += heartSpacing;
            }

            return;
        }

        // === Game Over Screen ===
        if (State == STATE.DEAD) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String message = "Game Over";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 - 50;
            g.drawString(message, x, y);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            FontMetrics restartFM = g.getFontMetrics();
            String restartMessage = "Press R to Restart";
            int restartX = (getWidth() - restartFM.stringWidth(restartMessage)) / 2;
            int restartY = y + restartFM.getHeight() + 60;
            g.setColor(Color.WHITE);
            g.drawString(restartMessage, restartX, restartY);
        }

        // === Mission Success Screen ===
        else if (State == STATE.MISSION_SUCCESS) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String message = "Mission Success!";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 - 50;
            g.drawString(message, x, y);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            FontMetrics restartFM = g.getFontMetrics();
            String restartMessage = "Press R to Restart";
            int restartX = (getWidth() - restartFM.stringWidth(restartMessage)) / 2;
            int restartY = y + restartFM.getHeight() + 60;
            g.setColor(Color.WHITE);
            g.drawString(restartMessage, restartX, restartY);
        }
    }

    // === Rendering Utilities ===

    /**
     * Renders the loading screen with a centered "Loading..." message.
     *
     * @param g The graphics context used for drawing
     */
    private void renderLoading(Graphics2D g) {
        // Fill the background with black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Set font and color for the loading message
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);

        // Center the text horizontally and vertically
        String message = "Loading...";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2;

        // Draw the loading message
        g.drawString(message, x, y);
    }

    /**
     * Renders the game’s start screen with a fade-in logo
     * and a prompt to click when the logo is fully visible.
     *
     * @param g The graphics context used for drawing
     */
    private void renderStarter(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // === Fill background with solid black ===
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // === Set transparency for logo fade-in effect ===
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(ac);

        // === Center and draw the logo image ===
        int logoX = (screenWidth - logo.getWidth(null)) / 2;
        int logoY = (screenHeight - logo.getHeight(null)) / 3;
        g2d.drawImage(logo, logoX, logoY, null);

        // === Display "Click Anywhere to Start" message when fade-in is complete ===
        if (alpha >= 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Full opacity
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

    /**
     * Draws a horizontally scrolling parallax background layer.
     *
     * @param g            The graphics context for rendering
     * @param img          The image used for the parallax layer
     * @param xo           The horizontal camera offset
     * @param scrollFactor The speed factor (higher = slower movement, creates depth)
     */
    private void drawParallaxLayer(Graphics2D g, Image img, int xo, int scrollFactor) {
        int layerWidth = img.getWidth(null);     // Width of the background image
        int layerHeight = img.getHeight(null);   // Height of the background image

        // Calculate where to start drawing the image on screen
        int drawX = (xo / scrollFactor) % layerWidth;
        if (drawX > 0) drawX -= layerWidth; // Shift left to start drawing earlier if needed

        // Tile the image across the screen to cover the full area
        for (int y = 0; y < getHeight(); y += layerHeight) {
            for (int x = drawX; x < getWidth(); x += layerWidth) {
                g.drawImage(img, x, y, null); // Draw repeated background image
            }
        }
    }

    /**
     * Checks if a sprite is within the visible screen area.
     * If visible, it is drawn and shown; otherwise, it is hidden.
     *
     * @param g  The graphics context used for drawing
     * @param s  The sprite to check and draw
     * @param xo The horizontal offset (camera X)
     * @param yo The vertical offset (camera Y)
     */
    public void checkSpriteVisible(Graphics2D g, Sprite s, int xo, int yo) {
        // Get the visible clipping bounds of the screen
        Rectangle rect = (Rectangle) g.getClip();

        // Calculate the sprite's screen position
        int xc = (int) (xo + s.getX());
        int yc = (int) (yo + s.getY());

        // Check if the sprite is within the visible area
        if (rect.contains(xc, yc)) {
            s.show();     // Make sprite visible
            s.draw(g);    // Draw it to screen
        } else {
            s.hide();     // Hide if off-screen (optimization)
        }
    }

    // === Input Handling ===

    /**
     * Handles key press events based on the current game state.
     * Controls player movement, attacks, state transitions, and level switching.
     *
     * @param e The key event triggered by the user
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();  // Get the key that was pressed

        // === In-Game Controls ===
        if (State == STATE.GAME) {
            // Quit the game
            if (key == KeyEvent.VK_ESCAPE) stop();

            // Player movement
            if (key == KeyEvent.VK_SPACE) jump = true;
            if (key == KeyEvent.VK_LEFT) left = true;
            if (key == KeyEvent.VK_RIGHT) right = true;

            // === Player Attack ===
            if (key == KeyEvent.VK_A && !attacking) {
                attacking = true;

                // Play attack sound and set attack animation based on direction
                new Sound("sounds/cyborg_attack.wav").start();
                if (lastDirectionRight) {
                    player.setAnimation(attack_right);
                } else {
                    player.setAnimation(attack_left);
                }

                // Delay before player can attack again
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                attacking = false;
                            }
                        }, 500 // Wait 500ms before allowing another attack
                );
            }

            // Switch to Level 1
            if (key == KeyEvent.VK_1) {
                levelNumber = 1;
                init("level1.txt");
                initialiseGame();
            }

            // Switch to Level 2
            if (key == KeyEvent.VK_2) {
                levelNumber = 2;
                init("level2.txt");
                initialiseGame();
            }

            // Return to Start Screen
            if (key == KeyEvent.VK_Q) {
                State = STATE.START;
            }
        }

        // === Restart from Dead State ===
        if (State == STATE.DEAD && key == KeyEvent.VK_R) {
            State = STATE.LOADING;
            loadingStartTime = System.currentTimeMillis(); // Start loading timer

            // Restart music from beginning
            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0); // Rewind
                midiSequencer.start();            // Play
            }
        }

        // === Restart After Mission Success ===
        if (State == STATE.MISSION_SUCCESS && key == KeyEvent.VK_R) {
            levelNumber = 1;  // Reset to level 1
            init("level1.txt");
            State = STATE.LOADING;
            loadingStartTime = System.currentTimeMillis();

            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0);
                midiSequencer.start();
            }
        }
    }

    /**
     * Handles key release events to stop movement or reset jump status.
     *
     * @param e The key event triggered when a key is released
     */
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode(); // Get the released key code

        // Respond based on which key was released
        switch (key) {

            // === ESC: Stop the game ===
            case KeyEvent.VK_ESCAPE -> stop();

            // === SPACE: End jump input ===
            case KeyEvent.VK_SPACE -> {
                jump = false;     // Player is no longer holding jump
                canJump = true;   // Allow jumping again
            }

            // === LEFT: Stop moving left and begin deceleration ===
            case KeyEvent.VK_LEFT -> {
                left = false;
                decelerate = true;
            }

            // === RIGHT: Stop moving right and begin deceleration ===
            case KeyEvent.VK_RIGHT -> {
                right = false;
                decelerate = true;
            }

            // === Any other key: Do nothing ===
            default -> {
                // No action for other keys
            }
        }
    }

    /**
     * Handles mouse click input during the START screen.
     * Begins the transition to the game if the logo has fully faded in.
     *
     * @param e The mouse event triggered by the player
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // Only respond to clicks on the START screen after fade-in is complete
        if (State == STATE.START && alpha >= 1.0f) {
            System.out.println("Mouse clicked: starting game...");
            State = STATE.LOADING;                     // Transition to loading screen
            loadingStartTime = System.currentTimeMillis(); // Start the loading timer
        }
    }

    // === Unused MouseListener Methods ===
    // These are required by the MouseListener interface but are not used in this game.
    @Override
    public void mouseClicked(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }

    // === Player Movement ===

    /**
     * Handles player movement input and physics including
     * gravity, jumping, walking, deceleration, and animation updates.
     *
     * @param elapsed The elapsed time since the last update (in milliseconds)
     */
    private void handlePlayerMovement(long elapsed) {
        // Apply gravity to player
        applyGravity(elapsed);

        // Do not process movement while attacking
        if (attacking) return;

        // === Standing Idle Animation ===
        if (touchingGround) {
            if (lastDirectionRight) {
                player.setAnimation(standing_right);
            } else {
                player.setAnimation(standing_left);
            }
        }

        // === Handle Jumping ===
        if (jump && canJump && touchingGround) {
            performJump();
        }

        // === Handle Horizontal Movement ===
        if (left) {
            moveLeft();
        }
        if (right) {
            moveRight();
        }

        // === Apply Deceleration if Movement Stops ===
        if (decelerate) {
            applyDeceleration();
        }

        // === Update Jumping/Falling Animation if Airborne ===
        updateAirborneAnimation();
    }

    /**
     * Applies gravity to the player by modifying vertical velocity over time.
     *
     * @param elapsed The time elapsed since the last frame (in milliseconds)
     */
    private void applyGravity(long elapsed) {
        float gravity = 0.0010f;
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
    }

    /**
     * Makes the player jump if allowed.
     * Plays jump sound and sets vertical velocity upwards.
     */
    private void performJump() {
        touchingGround = false;
        if (jumpsDone < 1) {
            if (player.getVelocityY() >= 0) {
                new Sound("sounds/cyborg_jump.wav").start();
                player.setVelocityY(-0.75f);  // Strong upward velocity
                player.shiftY(-0.01f);        // Slight offset to break ground contact
                jump = false;
                jumpsDone++;
            }
        } else {
            canJump = false;
        }
    }

    /**
     * Moves the player to the left, unless blocked by a wall.
     * Updates player velocity and animation.
     */
    private void moveLeft() {
        postX = player.getX() + player.getImage().getWidth(null);
        postY = player.getY() + (float) player.getImage().getHeight(null) / 2;

        if (isWallBlocking(postX - 0.02f, postY)) {
            player.setVelocityX(0);  // Stop if blocked
        } else {
            player.setVelocityX(-0.2f);        // Move left
            lastDirectionRight = false;        // Update last direction
            if (touchingGround) player.setAnimation(running_left);
        }
    }

    /**
     * Moves the player to the right, unless blocked by a wall.
     * Updates player velocity and animation.
     */
    private void moveRight() {
        postX = player.getX() + player.getImage().getWidth(null);
        postY = player.getY() + (float) player.getImage().getHeight(null) / 2;

        if (isWallBlocking(postX + 0.02f, postY)) {
            player.setVelocityX(0);  // Stop if blocked
        } else {
            player.setVelocityX(0.2f);         // Move right
            lastDirectionRight = true;         // Update last direction
            if (touchingGround) player.setAnimation(running_right);
        }
    }

    /**
     * Gradually slows down the player's horizontal movement when no key is pressed.
     */
    private void applyDeceleration() {
        player.setVelocityX(player.getVelocityX() * 0.9f);  // Reduce velocity

        // If nearly stopped, halt completely
        if (Math.abs(player.getVelocityX()) <= 0.01f) {
            player.setVelocityX(0);
            decelerate = false;
        }
    }

    /**
     * Updates the player's animation based on whether they are jumping or falling.
     * Only activates if the player is not touching the ground.
     */
    private void updateAirborneAnimation() {
        if (!touchingGround) {
            if (player.getVelocityY() > 0) {
                // Falling animation
                player.setAnimation(player.getPlayerDirection() ? falling_right : falling_left);
            } else if (player.getVelocityY() < 0) {
                // Jumping animation
                player.setAnimation(player.getPlayerDirection() ? jumping_right : jumping_left);
            }
        }
    }

    /**
     * Checks if the tile at the given position is a blocking wall tile.
     *
     * @param x The x-coordinate in the tilemap
     * @param y The y-coordinate in the tilemap
     * @return True if the tile blocks movement; false otherwise
     */
    private boolean isWallBlocking(float x, float y) {
        char tile = tmap.getTileChar((int)x, (int)y);
        return (tile == 'g' || tile == 'k' || tile == 'q' || tile == 'p' || tile == 'u' || tile == 'o');
    }

    // === Game Mechanics ===

    /**
     * Handles collisions between the player and enemies or the portal.
     * If the player is attacking and touches an enemy, the enemy is defeated.
     * If not attacking, the player takes damage and is knocked back.
     * Also checks if the player reaches the portal to finish the level.
     */
    private void handleSpriteCollisions() {

        // Only check collisions when in the GAME state
        if (State == STATE.GAME) {

            // Add all enemy sprites to a list for easier processing
            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            boolean collided = false; // Tracks if the player has collided with any enemy

            // === Check for collisions with enemies ===
            for (Sprite enemy : enemies) {
                // If there's a bounding box + circular collision with an enemy
                if (boundingBoxCollision(player, enemy) && BoundingCircleCollision(player, enemy)) {

                    if (attacking) {
                        // If attacking, defeat the enemy (play sound, hide, move off-screen)
                        Sound enemyDeath = new Sound("sounds/enemy_die.wav");
                        enemyDeath.start();
                        enemy.stop();
                        enemy.hide();
                        enemy.setX(-9999); // Move enemy off-screen
                        enemy.setY(-9999);
                    } else {
                        // If not attacking, register as a damaging collision
                        collided = true;
                    }
                }
            }

            // === Handle player damage if they collided with an enemy ===
            if (collided) {
                Sound damage = new Sound("sounds/cyborg_hurt.wav");
                damage.start();

                // Knockback logic based on direction player is facing
                if (player.getPlayerDirection()) {
                    // Facing right
                    player.setVelocityY(-0.2f);
                    player.setVelocityX(-0.2f);
                    player.setX(player.getX() - 10);
                    player.setY(player.getY() - 10);
                } else {
                    // Facing left
                    player.setVelocityY(0.2f);
                    player.setVelocityX(0.2f);
                    player.setX(player.getX() + 10);
                    player.setY(player.getY() - 10);
                }

                lifeRemaining--; // Reduce player's life
            }

            // === Check if player touches the portal and the portal is open ===
            if ((BoundingCircleCollision(player, portal)) && portalState.equals("Open")) {
                Sound levelComplete = new Sound("sounds/level_transition.wav");
                levelComplete.start();
                finishLevel(); // Transition to next level or mission success
            }
        }
    }

    /**
     * Handles interactions between a sprite and the tile map.
     * Detects and responds to collisions with ground, lava, coins, hearts, and level objectives.
     *
     * @param s The sprite being checked for tile collisions
     */
    public void handleTileMapCollisions(Sprite s) {
        // Convert sprite's position into tile coordinates
        int tileX = (int) (s.getX() / tmap.getTileWidth());
        int tileY = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight());

        // === Collision with solid ground tiles ===
        if (tmap.getTileChar(tileX, tileY) == 'G' || tmap.getTileChar(tileX, tileY) == 'T' ||
                tmap.getTileChar(tileX, tileY) == 'B' || tmap.getTileChar(tileX, tileY) == 'D' ||
                tmap.getTileChar(tileX, tileY) == 'L' || tmap.getTileChar(tileX, tileY) == 'R' ||
                tmap.getTileChar(tileX, tileY) == 'Q' || tmap.getTileChar(tileX, tileY) == 'W' ||
                tmap.getTileChar(tileX, tileY) == ']' || tmap.getTileChar(tileX, tileY) == '\\' ||
                tmap.getTileChar(tileX, tileY) == '/' || tmap.getTileChar(tileX, tileY) == '<' ||
                tmap.getTileChar(tileX, tileY) == '-' || tmap.getTileChar(tileX, tileY) == '>') {

            if (s.getVelocityY() > 0) {
                s.setVelocityY(0); // Stop downward movement
            }

            // Snap sprite to just above the tile it landed on
            s.setY((float) (tileY * tmap.getTileHeight()) - s.getHeight());

            if (s.equals(player)) {
                // Allow jumping again if this is the player
                jumpsDone = 0;
                touchingGround = true;
            }
        }

        // === Collision with lava tile 'V' ===
        if (tmap.getTileChar(tileX, tileY) == 'V') {
            if (s.equals(player)) {
                // Player takes damage and knockback
                Sound damage = new Sound("sounds/cyborg_hurt.wav");
                damage.start();

                if (player.getPlayerDirection()) {
                    player.setVelocityY(-0.2f);
                    player.setVelocityX(-0.2f);
                    player.setX(player.getX() - 10);
                } else {
                    player.setVelocityY(0.2f);
                    player.setVelocityX(0.2f);
                    player.setX(player.getX() + 10);
                }

                player.setY(player.getY() - 10);
                lifeRemaining--;

            } else {
                // Enemies stop and disappear in lava
                s.stop();
                s.hide();
            }
        }

        // === Coin collection: tile '1' ===
        if ((tmap.getTileChar(tileX, tileY - 1) == '1') && s.equals(player)) {
            Sound collect = new Sound("sounds/coin_collect.wav");
            collect.start();
            coinsCollected++;  // Increment player's coin count
            tmap.setTileChar('.', tileX, tileY - 1);  // Remove coin from map
        }

        // === Open portal when all coins collected ===
        if (coinsCollected == totalCoins) {
            portalState = "Open";
            portal.show();
        }

        // === Heart pickup: tile 'H' ===
        if ((tmap.getTileChar(tileX, tileY - 1) == 'H') && s.equals(player)) {
            if (lifeRemaining < 3) {
                tmap.setTileChar('.', tileX, tileY - 1); // Remove heart from map
                lifeRemaining++; // Restore one life
            }
        }
    }

    /**
     * Checks for collisions with solid tiles surrounding the sprite
     * (top, bottom, left, and right). Prevents movement through them.
     *
     * @param sprite the sprite to check collisions for
     */
    public void checkTileCollision(Sprite sprite) {

        // === Determine tile positions around the sprite ===
        xT = (int) ((sprite.getX() / tmap.getTileWidth()) + 0.5); // tile above
        yT = (int) (sprite.getY() / tmap.getTileHeight());

        xB = (int) (sprite.getX() / tmap.getTileWidth() + 0.5); // tile below
        yB = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 1.0);

        xR = (int) (sprite.getX() / tmap.getTileWidth() + 0.5); // tile to the right
        yR = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        xL = (int) (sprite.getX() / tmap.getTileWidth()); // tile to the left
        yL = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        // === Collision from below (head hitting ceiling) ===
        if ((tmap.getTileChar(xT, yT) == 'G' || tmap.getTileChar(xT, yT) == 'T' ||
                tmap.getTileChar(xT, yT) == 'B' || tmap.getTileChar(xT, yT) == 'D' ||
                tmap.getTileChar(xT, yT) == 'L' || tmap.getTileChar(xT, yT) == 'R' ||
                tmap.getTileChar(xT, yT) == 'Q' || tmap.getTileChar(xT, yT) == 'W' ||
                tmap.getTileChar(xT, yT) == ']' || tmap.getTileChar(xT, yT) == '\\' ||
                tmap.getTileChar(xT, yT) == '/' || tmap.getTileChar(xT, yT) == '<' ||
                tmap.getTileChar(xT, yT) == '-' || tmap.getTileChar(xT, yT) == '>') &&
                sprite.getVelocityY() > 0) {
            sprite.setVelocityY(0); // Stop upward movement
            canJump = false;        // Prevent jump if under a block
        } else {
            canJump = true; // Allow jump if nothing overhead
        }

        // === Collision from the right ===
        while ((tmap.getTileChar(xR, yR) == 'G' || tmap.getTileChar(xR, yR) == 'T' ||
                tmap.getTileChar(xR, yR) == 'B' || tmap.getTileChar(xR, yR) == 'D' ||
                tmap.getTileChar(xR, yR) == 'L' || tmap.getTileChar(xR, yR) == 'R' ||
                tmap.getTileChar(xR, yR) == 'Q' || tmap.getTileChar(xR, yR) == 'W' ||
                tmap.getTileChar(xR, yR) == ']' || tmap.getTileChar(xR, yR) == '\\' ||
                tmap.getTileChar(xR, yR) == '/' || tmap.getTileChar(xR, yR) == '<' ||
                tmap.getTileChar(xR, yR) == '-' || tmap.getTileChar(xR, yR) == '>') &&
                sprite.getVelocityX() > 0) {
            sprite.setVelocityX(0); // Stop rightward movement
            sprite.setX(xR * tmap.getTileWidth() - sprite.getImage().getWidth(null)); // Snap to left of wall
        }

        // === Collision from the left ===
        while ((tmap.getTileChar(xL, yL) == 'G' || tmap.getTileChar(xL, yL) == 'T' ||
                tmap.getTileChar(xL, yL) == 'B' || tmap.getTileChar(xL, yL) == 'D' ||
                tmap.getTileChar(xL, yL) == 'L' || tmap.getTileChar(xL, yL) == 'R' ||
                tmap.getTileChar(xL, yL) == 'Q' || tmap.getTileChar(xL, yL) == 'W' ||
                tmap.getTileChar(xL, yL) == ']' || tmap.getTileChar(xL, yL) == '\\' ||
                tmap.getTileChar(xL, yL) == '/' || tmap.getTileChar(xL, yL) == '<' ||
                tmap.getTileChar(xL, yL) == '-' || tmap.getTileChar(xL, yL) == '>') &&
                sprite.getVelocityX() < 0) {
            sprite.setVelocityX(0); // Stop leftward movement
            sprite.setX(xL * tmap.getTileWidth() + tmap.getTileWidth()); // Snap to right of wall
        }

        // === Collision from above (landing on a tile) ===
        if (tmap.getTileChar(xB, yB) == 'G' || tmap.getTileChar(xB, yB) == 'T' ||
                tmap.getTileChar(xB, yB) == 'B' || tmap.getTileChar(xB, yB) == 'D' ||
                tmap.getTileChar(xB, yB) == 'L' || tmap.getTileChar(xB, yB) == 'R' ||
                tmap.getTileChar(xB, yB) == 'Q' || tmap.getTileChar(xB, yB) == 'W' ||
                tmap.getTileChar(xB, yB) == ']' || tmap.getTileChar(xB, yB) == '\\' ||
                tmap.getTileChar(xB, yB) == '/' || tmap.getTileChar(xB, yB) == '<' ||
                tmap.getTileChar(xB, yB) == '-' || tmap.getTileChar(xB, yB) == '>') {
            sprite.setVelocityY(0); // Stop falling
            sprite.shiftY(2);       // Slightly push up to avoid re-collision
        }
    }

    /**
     * Handles collision with the screen or map boundaries to keep the sprite within bounds.
     * Applies bounce on bottom edge, and clamps left/right positions.
     *
     * @param s    The sprite to constrain
     * @param tmap The tile map to get screen size from
     */
    public void handleScreenEdge(Sprite s, TileMap tmap) {
        // === Bottom edge collision ===
        float bottomDifference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (bottomDifference > 0) {
            s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(bottomDifference));
            s.setVelocityY(-s.getVelocityY() * 0.75f); // Bounce upward with dampening
        }

        // === Left edge collision ===
        if (s.getX() < 0) {
            s.setX(0);             // Lock to screen's left
            s.setVelocityX(0);     // Stop leftward movement
        }

        // === Right edge collision ===
        float rightDifference = s.getX() + s.getWidth() - tmap.getPixelWidth();
        if (rightDifference > 0) {
            s.setX(tmap.getPixelWidth() - s.getWidth() - (int)(rightDifference)); // Lock to right edge
            s.setVelocityX(0); // Stop rightward movement
        }
    }

    /**
     * Resets enemy states and visibility based on the current level.
     * Sets animations, velocities, directions, and handles visibility for enemy4 (only appears in level 2).
     */
    private void resetEnemies() {
        // === Reset Enemy 1 ===
        enemy1.show();
        enemy1.setAnimation(enemy_running_right);
        enemy1.setVelocityX(0);
        enemy1.setVelocityY(0);

        // === Reset Enemy 2 ===
        enemy2.show();
        enemy2.setAnimation(enemy_running_right);
        enemy2.setVelocityX(0);
        enemy2.setVelocityY(0);

        // === Reset Enemy 3 ===
        enemy3.show();
        enemy3.setAnimation(enemy_running_right);
        enemy3.setVelocityX(0);
        enemy3.setVelocityY(0);

        // === Reset Enemy 4 (common properties, conditional visibility below) ===
        enemy4.setAnimation(enemy_running_right);
        enemy4.setVelocityX(0);
        enemy4.setVelocityY(0);

        // === Set all enemy directions to right ===
        enemy1.setDirection(true);
        enemy2.setDirection(true);
        enemy3.setDirection(true);
        enemy4.setDirection(true);

        // === Show or hide enemy4 depending on the level ===
        if (levelNumber == 2) {
            enemy4.show(); // Only visible in Level 2
        } else {
            enemy4.hide(); // Hidden and moved offscreen in Level 1
            enemy4.setX(-9999);
            enemy4.setY(-9999);
        }
    }


    // === Collision Detection ===

    /**
     * Checks for rectangular (AABB) collision between two sprites using bounding boxes.
     * Shrinks the boxes slightly to avoid early collision triggers (e.g. from transparent edges).
     *
     * @param s1 First sprite
     * @param s2 Second sprite
     * @return true if the bounding rectangles intersect
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2) {
        // Create smaller rectangles around both sprites
        Rectangle r1 = new Rectangle((int) s1.getX() + 5, (int) s1.getY() + 5, s1.getWidth() - 10, s1.getHeight() - 10);
        Rectangle r2 = new Rectangle((int) s2.getX() + 5, (int) s2.getY() + 5, s2.getWidth() - 10, s2.getHeight() - 10);

        // Check if they intersect
        return r1.intersects(r2);
    }

    /**
     * Checks for circular collision between two sprites using simplified radius overlap.
     * This method gives smoother, more forgiving collisions — useful for roundish sprites.
     *
     * @param one First sprite
     * @param two Second sprite
     * @return true if the distance between centers is less than the sum of radii
     */
    public boolean BoundingCircleCollision(Sprite one, Sprite two) {
        // Calculate distance between centers of the sprites
        int dx = ((int) one.getX() + one.getWidth() / 2) - ((int) two.getX() + two.getWidth() / 2);
        int dy = ((int) one.getY() + one.getHeight() / 2) - ((int) two.getY() + two.getHeight() / 2);

        // Calculate "effective radius" for each sprite (40% of width)
        double r1 = one.getWidth() * 0.4;
        double r2 = two.getWidth() * 0.4;

        // Compare squared distances (avoids costly square root)
        double distanceSquared = dx * dx + dy * dy;
        double radiusSum = r1 + r2;

        return distanceSquared < radiusSum * radiusSum;
    }


    /**
     * This method was only used to visualize which tiles are being collided with during development.
     * I will not remove it because the assignment does not permit us to remove methods.
     * As such, I will just leave it here.
     */
    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
//        if (collidedTiles.size() > 0)
//        {
//            int tileWidth = map.getTileWidth();
//            int tileHeight = map.getTileHeight();
//
//            g.setColor(Color.blue);
//            for (Tile t : collidedTiles)
//            {
//                g.drawRect(t.getXC()+xOffset, t.getYC()+yOffset, tileWidth, tileHeight);
//            }
//        }
    }

    // === UI/State Updates ===

    /**
     * Updates the alpha (transparency) of the start screen logo to create a fade-in effect.
     * Called repeatedly during the START state to gradually reveal the logo.
     */
    private void updateStarter() {
        // If we are in the fade-in phase and alpha is not fully opaque yet
        if (fadingIn && alpha < 1.0f) {
            alpha += 0.01f; // Gradually increase transparency

            // Clamp alpha to 1.0 and stop fading once fully visible
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fadingIn = false;
            }
        }
    }

    // === Helpers ===

    /**
     * Loads an image from the given path and scales it to 800x600.
     * This ensures all background images are consistent in size.
     *
     * @param path The file path to the image.
     * @return A BufferedImage scaled to screen size, or null if loading fails.
     */
    private BufferedImage loadAndScaleImage(String path) {
        try {
            // Load image asynchronously
            Image img = Toolkit.getDefaultToolkit().getImage(path);
            MediaTracker tracker = new MediaTracker(new java.awt.Container());
            tracker.addImage(img, 0);
            tracker.waitForID(0); // Wait until image is fully loaded

            // Create a blank image with transparency and draw the scaled image onto it
            BufferedImage scaled = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Smooth scaling
            g2d.drawImage(img, 0, 0, 800, 600, null);
            g2d.dispose();
            return scaled;
        } catch (Exception e) {
            e.printStackTrace(); // Print error details
            return null;
        }
    }

    /**
     * Loads an animation from a sprite sheet located in the animations folder.
     *
     * @param filename       The filename of the sprite sheet.
     * @param frames         The number of frames in the animation.
     * @param frameDuration  How long each frame lasts in milliseconds.
     * @return A fully loaded Animation object.
     */
    private Animation loadAnimation(String filename, int frames, int frameDuration) {
        Animation anim = new Animation();
        anim.loadAnimationFromSheet("images/Animations/" + filename, frames, 1, frameDuration);
        return anim;
    }

    /**
     * Required override from ActionListener. Currently unused.
     * Useful if you plan to trigger timed UI actions or add menus.
     *
     * @param e The action event (not used here).
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // No action required at this time
    }

    // === Enums ===

    /**
     * Represents the different states the game can be in.
     * This enum helps manage transitions between screens and behaviors.
     */
    public enum STATE {
        START,           // Initial start screen with logo and fade-in
        LOADING,         // Brief loading screen shown before the level begins
        GAME,            // Main gameplay state
        DEAD,            // Game over screen when player loses all lives
        MISSION_SUCCESS  // Screen shown after completing all levels
    }
}