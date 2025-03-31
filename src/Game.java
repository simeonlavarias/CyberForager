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
    private final int screenWidth = 800;
    private final int screenHeight = 600;

    private Sequencer midiSequencer;
    private boolean jump = false;
    private boolean left = false;
    private boolean right = false;
    private boolean decelerate = false;
    private boolean canJump = true;
    private boolean touchingGround = false;
    private boolean lastDirectionRight = true;
    private boolean attacking = false;
    private boolean fadingIn = true;
    private long loadingStartTime = 0;
    private float alpha = 0.0f;

    private int jumpsDone = 0;
    private int levelNumber = 1;
    private int coinsCollected = 0;
    private int totalCoins = 0;
    private int lifeRemaining = 3;

    private String portalState = ("Closed");
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

    private Sprite player = null;
    private Sprite enemy1 = null;
    private Sprite enemy2 = null;
    private Sprite enemy3 = null;
    private Sprite enemy4 = null;
    private Sprite portal = null;

    private final TileMap tmap = new TileMap();
    public float postX;
    public float postY;

    private Image[] parallaxLayersLevel1;
    private Image[] parallaxLayersLevel2;
    private Image heart1;
    private Image heart2;
    private Image heart3;
    private Image logo;

    int yT;
    int xT;
    int xB;
    int yB;
    int xR;
    int yR;
    int xL;
    int yL;

    public static STATE State = STATE.START;

    // Entry Point & Lifecycle of Game
    public static void main(String[] args) {
        Game game = new Game();
        game.init("level1.txt");
        try {
            game.midiSequencer = MidiSystem.getSequencer();
            game.midiSequencer.open();
            Sequence theme = MidiSystem.getSequence(new File("sounds/theme.mid"));
            game.midiSequencer.setSequence(theme);
            game.midiSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            game.midiSequencer.start();
            new Sound("sounds/game_start.wav").start();
        } catch (MidiUnavailableException | IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }

        int screenWidth = game.screenWidth;
        int screenHeight = game.screenHeight;
        game.run(false, screenWidth, screenHeight);
    }

    public void init(String map) {
        logo = new ImageIcon("images/Logo/cyber_forager_logo.png").getImage();
        if (levelNumber == 1) {
            tmap.loadMap("maps/level1", map);
        } else if (levelNumber == 2) {
            tmap.loadMap("maps/level2", map);
        }

        System.out.println("🗺️ [Game] Current levelNumber = " + levelNumber);

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

        enemy_running_left = loadAnimation("anim_enemy_running_left.png", 4, 400);
        enemy_running_right = loadAnimation("anim_enemy_running_right.png", 4, 400);

        portalAnimLevel1 = loadAnimation("portal1.png", 9, 50);
        portalAnimLevel2 = loadAnimation("portal2.png", 8, 50);

        player = new Sprite(standing_right);
        enemy1 = new Sprite(enemy_running_right);
        enemy2 = new Sprite(enemy_running_right);
        enemy3 = new Sprite(enemy_running_right);
        enemy4 = new Sprite(enemy_running_right);
        portal = new Sprite(portalAnimLevel1);

        initialiseGame();

        System.out.println(tmap);

        heart1 = loadImage("images/Heart.png").getScaledInstance(25, 22, Image.SCALE_DEFAULT);
        heart2 = heart1;
        heart3 = heart1;

        addMouseListener(this);
    }

    public void initialiseGame() {

        coinsCollected = 0;
        lifeRemaining = 3;

        if (levelNumber == 1)
        {
            totalCoins = 15;

            player.setX(tmap.getTileXC(3, 6));
            player.setY(tmap.getTileYC(3, 6));
            player.setVelocityX(0);
            player.setVelocityY(0);

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

            enemy4.hide();
            enemy4.stop();
            enemy4.setX(-9999);
            enemy4.setY(-9999);

            portal.setAnimation(portalAnimLevel1);
            portal.setX(tmap.getTileXC(61, 3));
            portal.setY(tmap.getTileYC(61, 3));
        }
        else if (levelNumber == 2)
        {
            File file = new File("maps/level2/level2.txt");
            if (!file.exists()) {
                System.out.println("ERROR: level2.txt file not found at " + file.getAbsolutePath());
            }

            totalCoins = 30;

            player.setX(tmap.getTileXC(1, 10));
            player.setY(tmap.getTileYC(1, 10));
            player.setVelocityX(0);
            player.setVelocityY(0);

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

            portal.setAnimation(portalAnimLevel2);
            portal.setX(tmap.getTileXC(31, 11));
            portal.setY(tmap.getTileYC(31, 11));
        }
        resetEnemies();
    }

    public void resetGame() {
        coinsCollected = 0;
        lifeRemaining = 3;

        tmap.loadMap("maps/level1", "level1.txt");

        totalCoins = 15;

        player.setX(tmap.getTileXC(3, 6));
        player.setY(tmap.getTileYC(3, 6));
        player.setVelocityX(0);
        player.setVelocityY(0);

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

        enemy4.hide();
        enemy4.stop();
        enemy4.setX(-9999);
        enemy4.setY(-9999);

        portal.setAnimation(portalAnimLevel1);
        portal.setX(tmap.getTileXC(61, 1));
        portal.setY(tmap.getTileYC(61, 1));

        resetEnemies();
    }

    public void finishLevel() {
        if (levelNumber == 1)
        {
            levelNumber = 2;
            System.out.println("Level 1 Done!");
            coinsCollected = 0;
            portalState = "Closed";
            System.out.println("🚀 [Game] Transitioning to Level 2...");
            init("level2.txt");
            initialiseGame();
        }
        else if (levelNumber == 2) {
            System.out.println("Mission Success");

            if (midiSequencer != null && midiSequencer.isRunning()) {
                midiSequencer.stop();
            }

            Sound sound = new Sound("win.wav");
            sound.echo("win.wav");

            levelNumber = 1;
            coinsCollected = 0;
            portalState = "Closed";
            Game.State = Game.STATE.MISSION_SUCCESS;
        }
    }

    // Game Loop
    public void update(long elapsed) {
        if (State == STATE.START) {
            updateStarter();
            return;
        }

        if (State == STATE.LOADING) {
            int loadingDuration = 5000;
            if (System.currentTimeMillis() - loadingStartTime >= loadingDuration) {
                State = STATE.GAME;
                initialiseGame();
            }
            return;
        }

        if (State == STATE.GAME)
        {
            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            for (Sprite s : sprites)
            {
                float gravity = 0.0010f;
                s.setVelocityY(s.getVelocityY() + (gravity * elapsed));
            }

            player.setAnimationSpeed(1.0f);
            checkTileCollision(player);

            handlePlayerMovement(elapsed);

            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            for (Sprite enemy : enemies)
            {
                if ((enemy.getX() > enemy.getMaxPatrol() && enemy.getDirection()) || (enemy.getX() < enemy.getMinPatrol() && !enemy.getDirection()))  // if the enemy hits the patrol area edge
                {
                    enemy.setDirection(!enemy.getDirection());
                }
                if (enemy.getDirection())
                {
                    enemy.setVelocityX(0.02f);
                    enemy.setAnimation(enemy_running_right);
                }
                else
                {
                    enemy.setVelocityX(-0.02f);
                    enemy.setAnimation(enemy_running_left);
                }
                enemy.update(elapsed);
            }

            for (Sprite s : sprites)
            {
                s.update(elapsed);
            }

            portal.update(elapsed);

            for (Sprite s : sprites)
            {
                handleTileMapCollisions(s);
            }

            handleScreenEdge(player, tmap);
            handleSpriteCollisions();

            if (lifeRemaining == 0)
            {
                Sound sound = new Sound("sounds/robot_death.wav");
                sound.start();
                System.out.println("You died!");
                levelNumber = 1;
                resetGame();
                Game.State = Game.STATE.DEAD;
                if (midiSequencer != null && midiSequencer.isRunning()) {
                    midiSequencer.stop();
                }
            }
        }
    }

    public void draw(Graphics2D g) {

        if (State == STATE.START) {
            renderStarter(g);
            return;
        }

        int xo = (int) -player.getX() + 150;
        int yo = (int) -player.getY() + 300;

        Image[] currentBackground = (levelNumber == 1) ? parallaxLayersLevel1 : parallaxLayersLevel2;

        drawParallaxLayer(g, currentBackground[0], xo, 12);
        drawParallaxLayer(g, currentBackground[1], xo, 9);
        drawParallaxLayer(g, currentBackground[2], xo, 6);
        drawParallaxLayer(g, currentBackground[3], xo, 3);
        drawParallaxLayer(g, currentBackground[4], xo, 2);

        if (State == STATE.LOADING) {
            renderLoading(g);
            return;
        }

        if (State == STATE.GAME) {

            ArrayList<Sprite> sprites = new ArrayList<>();
            sprites.add(player);
            sprites.add(enemy1);
            sprites.add(enemy2);
            sprites.add(enemy3);
            sprites.add(enemy4);

            tmap.draw(g, xo, yo);

            for (TileMap.DecorativeTile dt : tmap.getDecorativeTiles()) {
                g.drawImage(dt.image, dt.x + xo, dt.y + yo, null);
            }

            for (Sprite s : sprites) {
                s.setOffsets(xo, yo);
                checkOnScreen(g, s, xo, yo);
            }

            portal.setOffsets(xo, yo);
            portal.draw(g);

            g.setColor(Color.white);
            String coinMessage = String.format("Coins: %d / %d", coinsCollected, totalCoins);
            g.drawString(coinMessage, getWidth() - 170, 60);

            String portalMessage = "Portal: " + portalState;
            g.drawString(portalMessage, (getWidth() / 2) - 90, 60);

            int heartY = 40;
            int heartSpacing = screenWidth / 30;
            int heartX = screenWidth / 40;

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

    // Rendering Utilities
    private void renderLoading(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        String message = "Loading...";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        g.drawString(message, x, y);
    }

    private void renderStarter(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, screenWidth, screenHeight);

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

    public void checkOnScreen(Graphics2D g, Sprite s, int xo, int yo) {
        Rectangle rect = (Rectangle) g.getClip();
        int xc, yc;

        xc = (int) (xo + s.getX());
        yc = (int) (yo + s.getY());

        if (rect.contains(xc, yc))
        {
            s.show();
            s.draw(g);
        }
        else
        {
            s.hide();
        }
    }

    // Input Handling
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (State == STATE.GAME) {
            if (key == KeyEvent.VK_ESCAPE) stop();
            if (key == KeyEvent.VK_SPACE) jump = true;
            if (key == KeyEvent.VK_LEFT) left = true;
            if (key == KeyEvent.VK_RIGHT) right = true;

            if (key == KeyEvent.VK_A && !attacking) {
                attacking = true;

                new Sound("sounds/cyborg_attack.wav").start();
                if (lastDirectionRight) {
                    player.setAnimation(attack_right);
                } else {
                    player.setAnimation(attack_left);
                }

                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                attacking = false;
                            }
                        }, 500
                );
            }

            if (key == KeyEvent.VK_1) {
                levelNumber = 1;
                init("level1.txt");
                initialiseGame();
            }

            if (key == KeyEvent.VK_2) {
                levelNumber = 2;
                init("level2.txt");
                initialiseGame();
            }

            if (key == KeyEvent.VK_Q) {
                State = STATE.START;
            }
        }

        if (State == STATE.DEAD && key == KeyEvent.VK_R) {
            State = STATE.LOADING;
            loadingStartTime = System.currentTimeMillis();
            // No need to call initialiseGame here — it will happen after loading
            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0); // Rewind
                midiSequencer.start(); // Resume
            }
        }

        if (State == STATE.MISSION_SUCCESS && key == KeyEvent.VK_R) {
            levelNumber = 1;
            init("level1.txt");
            State = STATE.LOADING;
            loadingStartTime = System.currentTimeMillis();
            if (midiSequencer != null) {
                midiSequencer.setTickPosition(0);
                midiSequencer.start();
            }
        }
    }

    public void keyReleased(KeyEvent e) {

        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_ESCAPE -> stop();
            case KeyEvent.VK_SPACE -> {
                jump = false;
                canJump = true;
            }
            case KeyEvent.VK_LEFT -> {
                left = false;
                decelerate = true;
            }
            case KeyEvent.VK_RIGHT -> {
                right = false;
                decelerate = true;
            }
            default -> {
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (State == STATE.START && alpha >= 1.0f) {
            System.out.println("Mouse clicked: starting game...");
            State = STATE.LOADING;
            loadingStartTime = System.currentTimeMillis();
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

    // Player Movement

    private void handlePlayerMovement(long elapsed) {
        applyGravity(elapsed);

        if (attacking) return;

        if (touchingGround) {
            if (lastDirectionRight) {
                player.setAnimation(standing_right);
            } else {
                player.setAnimation(standing_left);
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

    private void applyGravity(long elapsed) {
        float gravity = 0.0010f;
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
    }

    private void performJump() {
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
            lastDirectionRight = false;
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
            lastDirectionRight = true;
            if (touchingGround) player.setAnimation(running_right);
        }
    }

    private void applyDeceleration() {
        player.setVelocityX(player.getVelocityX() * 0.9f);
        if (Math.abs(player.getVelocityX()) <= 0.01f) {
            player.setVelocityX(0);
            decelerate = false;
        }
    }

    private void updateAirborneAnimation() {
        if (!touchingGround) {
            if (player.getVelocityY() > 0) {
                player.setAnimation(player.getPlayerDirection() ? falling_right : falling_left);
            } else if (player.getVelocityY() < 0) {
                player.setAnimation(player.getPlayerDirection() ? jumping_right : jumping_left);
            }
        }
    }

    private boolean isWallBlocking(float x, float y) {
        char tile = tmap.getTileChar((int)x, (int)y);
        return (tile == 'g' || tile == 'k' || tile == 'q' || tile == 'p' || tile == 'u' || tile == 'o');
    }

    // Game Mechanics
    private void handleSpriteCollisions() {

        if (State == STATE.GAME)
        {
            ArrayList<Sprite> enemies = new ArrayList<>();
            enemies.add(enemy1);
            enemies.add(enemy2);
            enemies.add(enemy3);
            enemies.add(enemy4);

            boolean collided = false;

            for (Sprite enemy : enemies) {
                if (boundingBoxCollision(player, enemy) && BoundingCircleCollision(player, enemy)) {
                    if (attacking) {
                        Sound enemyDeath = new Sound("sounds/enemy_die.wav");
                        enemyDeath.start();
                        enemy.stop();
                        enemy.hide();
                        enemy.setX(-9999);
                        enemy.setY(-9999);
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

            if ((BoundingCircleCollision(player, portal)) && portalState.equals("Open"))
            {
                Sound levelComplete = new Sound("sounds/level_transition.wav");
                levelComplete.start();
                finishLevel();
            }
        }
    }

    public void handleTileMapCollisions(Sprite s) {
        int tileX = (int) (s.getX() / tmap.getTileWidth());
        int tileY = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight());

        if (tmap.getTileChar(tileX, tileY) == 'G' || tmap.getTileChar(tileX, tileY) == 'T' ||
                tmap.getTileChar(tileX, tileY) == 'B' || tmap.getTileChar(tileX, tileY) == 'D' ||
                tmap.getTileChar(tileX, tileY) == 'L' || tmap.getTileChar(tileX, tileY) == 'R' ||
                tmap.getTileChar(tileX, tileY) == 'Q' || tmap.getTileChar(tileX, tileY) == 'W' ||
                tmap.getTileChar(tileX, tileY) == ']' || tmap.getTileChar(tileX, tileY) == '\\' ||
                tmap.getTileChar(tileX, tileY) == '/' || tmap.getTileChar(tileX, tileY) == '<' ||
                tmap.getTileChar(tileX, tileY) == '-' || tmap.getTileChar(tileX, tileY) == '>')
        {
            if (s.getVelocityY() > 0)
            {
                s.setVelocityY(0);
            }
            s.setY((float) (tileY * tmap.getTileHeight()) - s.getHeight());
            if (s.equals(player))
            {
                jumpsDone = 0;
                touchingGround = true;
            }
        }

        if (tmap.getTileChar(tileX, tileY) == 'V')
        {
            if (s.equals(player))
            {
                Sound damage = new Sound("sounds/cyborg_hurt.wav");
                damage.start();
                if (player.getPlayerDirection())
                {
                    player.setVelocityY(-0.2f);
                    player.setVelocityX(-0.2f);
                    player.setX(player.getX() - 10);
                }
                else
                {
                    player.setVelocityY(0.2f);
                    player.setVelocityX(0.2f);
                    player.setX(player.getX() + 10);
                }
                player.setY(player.getY() - 10);
                lifeRemaining--;

            } else {
                s.stop();
                s.hide();
            }
        }

        if ((tmap.getTileChar(tileX, tileY - 1) == '1') && s.equals(player))
        {
            Sound collect = new Sound("sounds/coin_collect.wav");
            collect.start();
            coinsCollected++;
            tmap.setTileChar('.', tileX, tileY - 1);
        }

        if (coinsCollected == totalCoins)
        {
            portalState = ("Open");
            portal.show();
        }
        if ((tmap.getTileChar(tileX, tileY - 1) == 'H') && s.equals(player))
        {
            if (lifeRemaining < 3)
            {
                tmap.setTileChar('.', tileX, tileY - 1);
                lifeRemaining++;
            }
        }
    }

    public void checkTileCollision(Sprite sprite) {

        xT = (int) ((sprite.getX() / tmap.getTileWidth()) + 0.5);
        yT = (int) (sprite.getY() / tmap.getTileHeight());

        xB = (int) (sprite.getX() / tmap.getTileWidth() + 0.5);
        yB = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 1.0);

        xR = (int) (sprite.getX() / tmap.getTileWidth() + 0.5);
        yR = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        xL = (int) (sprite.getX() / tmap.getTileWidth());
        yL = (int) ((sprite.getY() + sprite.getHeight()) / tmap.getTileHeight() - 0.75);

        if ((tmap.getTileChar(xT, yT) == 'G' || tmap.getTileChar(xT, yT) == 'T' ||
                tmap.getTileChar(xT, yT) == 'B' || tmap.getTileChar(xT, yT) == 'D' ||
                tmap.getTileChar(xT, yT) == 'L' || tmap.getTileChar(xT, yT) == 'R' ||
                tmap.getTileChar(xT, yT) == 'Q' || tmap.getTileChar(xT, yT) == 'W' ||
                tmap.getTileChar(xT, yT) == ']' || tmap.getTileChar(xT, yT) == '\\' ||
                tmap.getTileChar(xT, yT) == '/' || tmap.getTileChar(xT, yT) == '<' ||
                tmap.getTileChar(xT, yT) == '-' || tmap.getTileChar(xT, yT) == '>') &&
                sprite.getVelocityY() > 0) {
            sprite.setVelocityY(0);
            canJump = false;
        }
        else
        {
            canJump = true;
        }
        while ((tmap.getTileChar(xR, yR) == 'G' || tmap.getTileChar(xR, yR) == 'T' ||
                tmap.getTileChar(xR, yR) == 'B' || tmap.getTileChar(xR, yR) == 'D' ||
                tmap.getTileChar(xR, yR) == 'L' || tmap.getTileChar(xR, yR) == 'R' ||
                tmap.getTileChar(xR, yR) == 'Q' || tmap.getTileChar(xR, yR) == 'W' ||
                tmap.getTileChar(xR, yR) == ']' || tmap.getTileChar(xR, yR) == '\\' ||
                tmap.getTileChar(xR, yR) == '/' || tmap.getTileChar(xR, yR) == '<' ||
                tmap.getTileChar(xR, yR) == '-' || tmap.getTileChar(xR, yR) == '>') &&
                sprite.getVelocityX() > 0) {
            sprite.setVelocityX(0);
            sprite.setX(xR * tmap.getTileWidth() - sprite.getImage().getWidth(null));
        }
        while ((tmap.getTileChar(xL, yL) == 'G' || tmap.getTileChar(xL, yL) == 'T' ||
                tmap.getTileChar(xL, yL) == 'B' || tmap.getTileChar(xL, yL) == 'D' ||
                tmap.getTileChar(xL, yL) == 'L' || tmap.getTileChar(xL, yL) == 'R' ||
                tmap.getTileChar(xL, yL) == 'Q' || tmap.getTileChar(xL, yL) == 'W' ||
                tmap.getTileChar(xL, yL) == ']' || tmap.getTileChar(xL, yL) == '\\' ||
                tmap.getTileChar(xL, yL) == '/' || tmap.getTileChar(xL, yL) == '<' ||
                tmap.getTileChar(xL, yL) == '-' || tmap.getTileChar(xL, yL) == '>') &&
                sprite.getVelocityX() < 0) {
            sprite.setVelocityX(0);
            sprite.setX(xL * tmap.getTileWidth() + tmap.getTileWidth());
        }
        if (tmap.getTileChar(xB, yB) == 'G' || tmap.getTileChar(xB, yB) == 'T' ||
                tmap.getTileChar(xB, yB) == 'B' || tmap.getTileChar(xB, yB) == 'D' ||
                tmap.getTileChar(xB, yB) == 'L' || tmap.getTileChar(xB, yB) == 'R' ||
                tmap.getTileChar(xB, yB) == 'Q' || tmap.getTileChar(xB, yB) == 'W' ||
                tmap.getTileChar(xB, yB) == ']' || tmap.getTileChar(xB, yB) == '\\' ||
                tmap.getTileChar(xB, yB) == '/' || tmap.getTileChar(xB, yB) == '<' ||
                tmap.getTileChar(xB, yB) == '-' || tmap.getTileChar(xB, yB) == '>') {
            sprite.setVelocityY(0);
            sprite.shiftY(2);
        }
    }

    public void handleScreenEdge(Sprite s, TileMap tmap) {
        float bottomDifference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (bottomDifference > 0)
        {
            s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(bottomDifference));
            s.setVelocityY(-s.getVelocityY() * 0.75f); // bounce up
        }

        if (s.getX() < 0)
        {
            s.setX(0);
            s.setVelocityX(0);
        }

        float rightDifference = s.getX() + s.getWidth() - tmap.getPixelWidth();
        if (rightDifference > 0)
        {
            s.setX(tmap.getPixelWidth() - s.getWidth() - (int)(rightDifference));
            s.setVelocityX(0);
        }
    }

    private void resetEnemies() {
        enemy1.show();
        enemy1.setAnimation(enemy_running_right);
        enemy1.setVelocityX(0);
        enemy1.setVelocityY(0);

        enemy2.show();
        enemy2.setAnimation(enemy_running_right);
        enemy2.setVelocityX(0);
        enemy2.setVelocityY(0);

        enemy3.show();
        enemy3.setAnimation(enemy_running_right);
        enemy3.setVelocityX(0);
        enemy3.setVelocityY(0);

        enemy4.setAnimation(enemy_running_right);
        enemy4.setVelocityX(0);
        enemy4.setVelocityY(0);

        enemy1.setDirection(true);
        enemy2.setDirection(true);
        enemy3.setDirection(true);
        enemy4.setDirection(true);

        if (levelNumber == 2) {
            enemy4.show();
        } else {
            enemy4.hide();
            enemy4.setX(-9999);
            enemy4.setY(-9999);
        }
    }

    // Collision Detection
    public boolean boundingBoxCollision(Sprite s1, Sprite s2) {
        Rectangle r1 = new Rectangle((int) s1.getX() + 5, (int) s1.getY() + 5, s1.getWidth() - 10, s1.getHeight() - 10);
        Rectangle r2 = new Rectangle((int) s2.getX() + 5, (int) s2.getY() + 5, s2.getWidth() - 10, s2.getHeight() - 10);
        return r1.intersects(r2);
    }

    public boolean BoundingCircleCollision(Sprite one, Sprite two) {
        int dx = ((int) one.getX() + one.getWidth() / 2) - ((int) two.getX() + two.getWidth() / 2);
        int dy = ((int) one.getY() + one.getHeight() / 2) - ((int) two.getY() + two.getHeight() / 2);

        double r1 = one.getWidth() * 0.4;
        double r2 = two.getWidth() * 0.4;

        double distanceSquared = dx * dx + dy * dy;
        double radiusSum = r1 + r2;

        return distanceSquared < radiusSum * radiusSum;
    }

    /**
     * The code below was only used to visualize which tiles are being collided with
     * during development. I will not remove it because the assignment does not permit
     * us to remove methods, so I will just leave it here.
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

    // UI/State Updates
    private void updateStarter() {
        if (fadingIn && alpha < 1.0f) {
            alpha += 0.01f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fadingIn = false;
            }
        }
    }

    // Helpers
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
    private Animation loadAnimation(String filename, int frames, int frameDuration) {
        Animation anim = new Animation();
        anim.loadAnimationFromSheet("images/Animations/" + filename, frames, 1, frameDuration);
        return anim;
    }
    @Override
    public void actionPerformed(ActionEvent e) {

    }

    // Enums
    public enum STATE {
        START,
        LOADING,
        GAME,
        DEAD,
        MISSION_SUCCESS
    }
}