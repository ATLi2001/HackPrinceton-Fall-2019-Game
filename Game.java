import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Game {

    enum key {
        LEFT, RIGHT, UP, DOWN, W, A, S, D;
    }

    //current key that is expected to be hit
    public key currKey;

    public int score = 0;
    private int fps = 60;
    private int frameCount = 0;
    private int highScore = 0;
    private boolean running = true;
    private boolean startAgain = true;
    private int color = 0;

    //game panel with animation
    private GamePanel gamePanel = new GamePanel();
    public JFrame frame;

    //constructor
    public Game() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(3, 3));

        JLabel topleft = new JLabel("HackPrincetonFall2019");
        frame = new JFrame();

        p.add(topleft);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(400, 800));
        frame.getContentPane().add(BorderLayout.NORTH, p);
        frame.getContentPane().add(BorderLayout.CENTER, gamePanel);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.setFocusable(true);
        frame.addKeyListener(new MyKeyListener());


        try {
            Scanner scan = new Scanner(new File("highScore.txt"));
            if (scan.hasNextInt())
                highScore = scan.nextInt();
        }
        catch (Exception e) {
            System.out.println(e);
        }

        runGameLoop();
    }

    public static void main(String[] args) {
        Game game = new Game();

    }


    //Starts a new thread and runs the game loop in it.
    public void runGameLoop() {
        Thread loop = new Thread() {
            public void run() {
                gameLoop();
            }
        };
        loop.start();
    }

    //Only run this in another Thread!
    private void gameLoop() {
        //This value would probably be stored elsewhere.
        final double GAME_HERTZ = 20.0;
        //Calculate how many ns each frame should take for our target game hertz.
        final double TIME_BETWEEN_UPDATES = 1000000000 / GAME_HERTZ;
        //At the very most we will update the game this many times before a new render.
        //If you're worried about visual hitches more than perfect timing, set this to 1.
        final int MAX_UPDATES_BEFORE_RENDER = 5;
        //We will need the last update time.
        double lastUpdateTime = System.nanoTime();
        //Store the last time we rendered.
        double lastRenderTime = System.nanoTime();

        //If we are able to get as high as this FPS, don't render again.
        final double TARGET_FPS = 60;
        final double TARGET_TIME_BETWEEN_RENDERS = 1000000000 / TARGET_FPS;

        //Simple way of finding FPS.
        int lastSecondTime = (int) (lastUpdateTime / 1000000000);

        while (running) {
            double now = System.nanoTime();
            int updateCount = 0;

            if (startAgain) {
                gamePanel.startAgain();
                startAgain = false;
            }

            if (!startAgain) {
                //Do as many game updates as we need to, potentially playing catchup.
                while (now - lastUpdateTime > TIME_BETWEEN_UPDATES
                        && updateCount < MAX_UPDATES_BEFORE_RENDER) {
                    updateGame();
                    lastUpdateTime += TIME_BETWEEN_UPDATES;
                    updateCount++;
                }

                //If for some reason an update takes forever, we don't want to do an insane number of catchups.
                //If you were doing some sort of game that needed to keep EXACT time, you would get rid of this.
                if (now - lastUpdateTime > TIME_BETWEEN_UPDATES) {
                    lastUpdateTime = now - TIME_BETWEEN_UPDATES;
                }

                //Render. To do so, we need to calculate interpolation for a smooth render.
                float interpolation = Math
                        .min(1.0f, (float) ((now - lastUpdateTime) / TIME_BETWEEN_UPDATES));
                drawGame(interpolation);
                lastRenderTime = now;

                //Update the frames we got.
                int thisSecond = (int) (lastUpdateTime / 1000000000);
                if (thisSecond > lastSecondTime) {

                    //  System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
                    // fps = 30;
                    // frameCount = 0;
                    lastSecondTime = thisSecond;
                }

                //Yield until it has been at least the target time between renders. This saves the CPU from hogging.
                while (now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS
                        && now - lastUpdateTime < TIME_BETWEEN_UPDATES) {
                    Thread.yield();

                    //This stops the app from consuming all your CPU. It makes this slightly less accurate, but is worth it.
                    //You can remove this line and it will still work (better), your CPU just climbs on certain OSes.
                    //FYI on some OS's this can cause pretty bad stuttering. Scroll down and have a look at different peoples' solutions to this.
                    try {
                        Thread.sleep(1);
                    }
                    catch (Exception e) {
                    }

                    now = System.nanoTime();
                }
            }
        }
    }

    private void updateGame() {
        gamePanel.update();
    }

    private void drawGame(float interpolation) {
        gamePanel.setInterpolation(interpolation);
        gamePanel.repaint();
    }


    public String randomValue() {
        int rand = (int) (Math.random() * 8);

        if (rand == 0) {
            currKey = key.UP;
            return "^";
        }
        if (rand == 1) {
            currKey = key.RIGHT;
            return ">";
        }
        if (rand == 2) {
            currKey = key.LEFT;
            return "<";
        }
        if (rand == 3) {
            currKey = key.DOWN;
            return "v";
        }
        if (rand == 4) {
            currKey = key.W;
            return "W";
        }
        if (rand == 5) {
            currKey = key.A;
            return "A";
        }
        if (rand == 6) {
            currKey = key.S;
            return "S";
        }
        if (rand == 7) {
            currKey = key.D;
            return "D";
        }

        return "";

    }

    public void quit() {
        int result = JOptionPane.showConfirmDialog(null, "Your Score is "
                + score, "Confirm Quit", JOptionPane.DEFAULT_OPTION);
        if (score > highScore) {
            File fold = new File("highScore.txt");
            fold.delete();
            File fnew = new File("highScore.txt");
            try {
                FileWriter f2 = new FileWriter(fnew, false);
                f2.write(score + "");
                f2.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (result == 0) System.exit(0);
    }

    private class MyKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {

            int key = e.getKeyCode();
            char c = e.getKeyChar();

            if (key == KeyEvent.VK_LEFT) {
                checkLeft();
            }

            if (key == KeyEvent.VK_RIGHT) {
                checkRight();
            }

            if (key == KeyEvent.VK_UP) {
                checkUp();
            }

            if (key == KeyEvent.VK_DOWN) {
                checkDown();
            }
            if (c == 'w') {
                checkW();
            }
            if (c == 'a') {
                checkA();
            }
            if (c == 's') {
                checkS();
            }
            if (c == 'd') {
                checkD();
            }
        }

        public void checkLeft() {
            if (currKey == key.LEFT) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkRight() {
            if (currKey == key.RIGHT) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkDown() {
            if (currKey == key.DOWN) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkUp() {
            if (currKey == key.UP) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkW() {
            if (currKey == key.W) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkA() {
            if (currKey == key.A) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkS() {
            if (currKey == key.S) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }

        public void checkD() {
            if (currKey == key.D) {
                score++;
                startAgain = true;
            }
            else {
                quit();
            }
        }
    }


    private class GamePanel extends JPanel {
        float interpolation;
        float ballX, ballY, lastBallX, lastBallY;
        int ballWidth, ballHeight;
        float ballYVel;
        float ballSpeed;

        String keyVal;


        int lastDrawX, lastDrawY;

        public GamePanel() {
            ballX = lastBallX = 200;
            ballY = lastBallY = 100;
            ballWidth = 75;
            ballHeight = 50;
            ballSpeed = 15;
            ballYVel = ballSpeed;
            keyVal = randomValue();
        }

        public void setInterpolation(float interp) {
            interpolation = interp;
        }

        //collisions
        public void update() {
            lastBallY = ballY;
            ballY += ballYVel;

            if (ballY - ballHeight / 2 > getHeight()) {
                quit();
                // end game here as well
            }
            //keyVal = randomValue();
        }

        public void startAgain() {
            ballY = 100;
            lastBallY = 100;
            if (score == 5) ballYVel += 5;
            if (score == 10) ballYVel += 5;
            if (score > 20) ballYVel += 2;
            keyVal = randomValue();
        }

        public void paintComponent(Graphics g) {

            if (color >= 255)
                color = 0;

            color += 1;
            // int col = (int) (Math.random()*255);
            setBackground(new Color(color, 120, 150));
            //setBorder(Color.BLUE);
            //BS way of clearing out the old rectangle to save CPU.
            g.setColor(getBackground());
            g.fillRect(lastDrawX - 1, lastDrawY - 1, ballWidth + 2, ballHeight + 2);
            g.fillRect(5, 0, 75, 30);

            // if (frameCount % 2 == 0)
            //     g.setColor(Color.BLUE);
            // else
            //     g.setColor(Color.RED);
            g.setColor(Color.BLUE);
            int drawX = (int) ((ballX - lastBallX) * interpolation + lastBallX - ballWidth / 2);
            int drawY = (int) ((ballY - lastBallY) * interpolation + lastBallY - ballHeight / 2);
            g.drawRect(drawX, drawY, ballWidth, ballHeight);
            g.setFont(new Font("ComicSans", Font.BOLD, 30));
            g.drawString(keyVal, drawX + ballWidth / 2 - 11, drawY + ballHeight / 2);

            lastDrawY = drawY;

            g.setColor(Color.BLACK);

            g.setFont(new Font("ComicSans", Font.BOLD, 10));
            g.drawString("High Score: " + highScore + "\n" + "Score: " + score, 10, 20);
            // g.drawString("Score: " + score, 5, 15);

            frameCount++;
        }
    }


}





