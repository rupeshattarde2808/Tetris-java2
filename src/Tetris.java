import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

/**
 * Tetris - a classic block-stacking puzzle game.
 * Controls: LEFT/RIGHT = move, UP = rotate, DOWN = soft drop,
 *           SPACE = hard drop, P = pause, R = restart after game over
 */
public class Tetris extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tetris game = new Tetris();
            game.setVisible(true);
        });
    }

    public Tetris() {
        setTitle("Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        String[] options = {"Easy", "Normal", "Hard"};

    int choice = JOptionPane.showOptionDialog(
            this,
            "Select Difficulty Level:",
            "Tetris Difficulty",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]
    );

    Board board;

    if (choice == 0) {
        board = new Board("Easy");
    } else if (choice == 2) {
        board = new Board("Hard");
    } else {
        board = new Board("Medium");
    }
        add(board);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(board);
        setFocusable(true);
    }
}

class Board extends JPanel implements KeyListener {
    static final int COLS = 10;
    static final int ROWS = 20;
    static final int TILE = 30;
    static final int SIDEBAR = 150;

    static final int[][][] SHAPES = {
        // I
        {{0,1},{1,1},{2,1},{3,1}},
        // O
        {{1,0},{2,0},{1,1},{2,1}},
        // T
        {{1,0},{0,1},{1,1},{2,1}},
        // S
        {{1,0},{2,0},{0,1},{1,1}},
        // Z
        {{0,0},{1,0},{1,1},{2,1}},
        // J
        {{0,0},{0,1},{1,1},{2,1}},
        // L
        {{2,0},{0,1},{1,1},{2,1}}
    };

    static final Color[] COLORS = {
        Color.CYAN, Color.YELLOW, new Color(160, 32, 240),
        Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE
    };

    private Color[][] grid = new Color[ROWS][COLS];
    private int[][] currentShape;
    private Color currentColor;
    private int curX, curY;
    private int curType;

    private int[][] nextShape;
    private Color nextColor;
    private int nextType;

    private Timer timer;
    private int delay = 500;
    private boolean gameOver = false;
    private boolean paused = false;
    private int score = 0;
    private int highScore = 0;
    private int lines = 0;
    private int level = 1;
    private String difficulty;
    private int startingDelay;
    private final String HIGH_SCORE_FILE = "highscore.txt";
    private Clip backgroundMusic;
    private Random rand = new Random();

    public Board(String difficulty) {
        setPreferredSize(new Dimension(COLS * TILE + SIDEBAR, ROWS * TILE));
        setBackground(Color.BLACK);

        this.difficulty=difficulty;

        if(difficulty.equals("Easy")){
            startingDelay=500;
        }else if(difficulty.equals("Hard")){
            startingDelay=150;
        }else{
            startingDelay=300;
        }
        initGame();
    }

    private void initGame() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = null;

        score = 0;
        highScore = loadHighScore();
        lines = 0;
        level = 1;
        delay = startingDelay;
        gameOver = false;
        paused = false;

        spawnNext();
        spawnPiece();

        if (timer != null) timer.stop();
        timer = new Timer(delay, e -> gameLoop());
        timer.start();
        playBackgroundMusic();
    }

    private int loadHighScore() {
    try {
        File file = new File(HIGH_SCORE_FILE);

        if (!file.exists()) {
            return 0;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        reader.close();

        if (line != null) {
            return Integer.parseInt(line);
        }

    } catch (Exception e) {
        System.out.println("Could not load high score.");
    }

    return 0;
}
    private void saveHighScore() {
    try {
        FileWriter writer = new FileWriter(HIGH_SCORE_FILE);
        writer.write(String.valueOf(highScore));
        writer.close();

    } catch (IOException e) {
        System.out.println("Could not save high score.");
    }
}


    private void gameLoop() {
        if (!paused && !gameOver) {
            moveDown();
        }
        repaint();
    }

    private void spawnNext() {
        nextType = rand.nextInt(SHAPES.length);
        nextShape = deepCopy(SHAPES[nextType]);
        nextColor = COLORS[nextType];
    }

    private void spawnPiece() {
        currentShape = nextShape;
        currentColor = nextColor;
        curType = nextType;
        spawnNext();

        curX = COLS / 2 - 2;
        curY = 0;

        if (collides(currentShape, curX, curY)) {
            gameOver = true;
            timer.stop();
            stopBackgroundMusic();
            playGameOverSound();
        }
    }

    private int[][] deepCopy(int[][] shape) {
        int[][] copy = new int[shape.length][2];
        for (int i = 0; i < shape.length; i++) {
            copy[i][0] = shape[i][0];
            copy[i][1] = shape[i][1];
        }
        return copy;
    }

    private boolean collides(int[][] shape, int offX, int offY) {
        for (int[] cell : shape) {
            int x = cell[0] + offX;
            int y = cell[1] + offY;
            if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return true;
            if (y >= 0 && grid[y][x] != null) return true;
        }
        return false;
    }

    private void moveDown() {
        if (!collides(currentShape, curX, curY + 1)) {
            curY++;
        } else {
            lockPiece();
            playLockSound();            
            clearLines();
            spawnPiece();
        }
    }

    private void hardDrop() {
        while (!collides(currentShape, curX, curY + 1)) {
            curY++;
            score += 1;
        }
        lockPiece();
        playHardDropSound();
        clearLines();
        spawnPiece();
        repaint();
    }

    private void lockPiece() {
        for (int[] cell : currentShape) {
            int x = cell[0] + curX;
            int y = cell[1] + curY;
            if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                grid[y][x] = currentColor;
            }
        }
    }

    private void clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == null) { full = false; break; }
            }
            if (full) {
                cleared++;
                for (int rr = r; rr > 0; rr--) {
                    grid[rr] = grid[rr - 1].clone();
                }
                grid[0] = new Color[COLS];
                r++; // re-check same row index after shift
            }
        }
        if (cleared > 0) {
            if (cleared == 4) {
               playTetrisSound();
               } else {
                playLineClearSound();
                }
            lines += cleared;
            switch (cleared) {
                case 1: score += 100 * level; break;
                case 2: score += 300 * level; break;
                case 3: score += 500 * level; break;
                case 4: score += 800 * level; break;
            }

            if(score > highScore){
                highScore = score;
                saveHighScore();
            }
            int newLevel = lines / 10 + 1;
            if (newLevel != level) {
                level = newLevel;
                delay = Math.max(100, startingDelay - (level - 1) * 40);
                timer.setDelay(delay);
            }
        }
    }

    private void rotate() {
        int[][] rotated = new int[currentShape.length][2];
        // Rotate around approximate center (using shape index 1 as pivot-ish, simple rotation)
        for (int i = 0; i < currentShape.length; i++) {
            int x = currentShape[i][0];
            int y = currentShape[i][1];
            rotated[i][0] = 1 - y + 1; // pivot offset
            rotated[i][1] = x;
        }
        // O piece (square) shouldn't rotate visually
        if (curType == 1) return;

        if (!collides(rotated, curX, curY)) {
            currentShape = rotated;
        } else if (!collides(rotated, curX - 1, curY)) {
            curX -= 1;
            currentShape = rotated;
        } else if (!collides(rotated, curX + 1, curY)) {
            curX += 1;
            currentShape = rotated;
        }
    }

    private void moveLeft() {
        if (!collides(currentShape, curX - 1, curY)) curX--;
    }

    private void moveRight() {
        if (!collides(currentShape, curX + 1, curY)) curX++;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw board border
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(0, 0, COLS * TILE, ROWS * TILE);

        // Draw locked blocks
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    drawTile(g2, c, r, grid[r][c]);
                }
            }
        }

        // Draw current piece
        if (!gameOver && currentShape != null) {
            for (int[] cell : currentShape) {
                drawTile(g2, cell[0] + curX, cell[1] + curY, currentColor);
            }
        }

// Sidebar
int sx = COLS * TILE + 15;
g2.setColor(Color.WHITE);
g2.setFont(new Font("Arial", Font.BOLD, 16));

g2.drawString("SCORE", sx, 30);
g2.drawString(String.valueOf(score), sx, 50);

g2.drawString("HIGH SCORE", sx, 85);
g2.drawString(String.valueOf(highScore), sx, 105);

g2.drawString("LINES", sx, 140);
g2.drawString(String.valueOf(lines), sx, 160);

g2.drawString("LEVEL", sx, 195);
g2.drawString(String.valueOf(level), sx, 215);

g2.drawString("DIFFICULTY", sx, 250);
g2.drawString(difficulty, sx, 270);

g2.drawString("NEXT", sx, 310);

if (nextShape != null) {
    for (int[] cell : nextShape) {
        int px = sx + cell[0] * (TILE - 5);
        int py = 320 + cell[1] * (TILE - 5);

        g2.setColor(nextColor);
        g2.fillRect(px, py, TILE - 6, TILE - 6);

        g2.setColor(Color.BLACK);
        g2.drawRect(px, py, TILE - 6, TILE - 6);
    }
}

        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.drawString("Arrows: move/rotate", sx, 450);
        g2.drawString("Space: hard drop", sx, 470);
        g2.drawString("P: pause", sx, 490);
        g2.drawString("R: restart", sx, 510);

        if (paused && !gameOver) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, COLS * TILE, ROWS * TILE);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("PAUSED", COLS * TILE / 2 - 60, ROWS * TILE / 2);
        }

        if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, COLS * TILE, ROWS * TILE);
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("GAME OVER", COLS * TILE / 2 - 80, ROWS * TILE / 2 - 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.drawString("Press R to restart", COLS * TILE / 2 - 65, ROWS * TILE / 2 + 20);
        }
    }

    private void drawTile(Graphics2D g2, int x, int y, Color color) {
        int px = x * TILE;
        int py = y * TILE;
        g2.setColor(color);
        g2.fillRect(px, py, TILE, TILE);
        g2.setColor(color.darker());
        g2.drawRect(px, py, TILE - 1, TILE - 1);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawLine(px + 1, py + 1, px + TILE - 2, py + 1);
    }
    private void playSound(int frequency, int duration) {
    new Thread(() -> {
        try {
            float sampleRate = 44100;

            AudioFormat format = new AudioFormat(
                    sampleRate,
                    8,
                    1,
                    true,
                    false
            );

            SourceDataLine line =
                    AudioSystem.getSourceDataLine(format);

            line.open(format);
            line.start();

            byte[] buffer = new byte[1];

            int samples =
                    (int) (duration * sampleRate / 1000);

            for (int i = 0; i < samples; i++) {

                double angle =
                        2.0 * Math.PI * frequency * i / sampleRate;

                buffer[0] =
                        (byte) (Math.sin(angle) * 100);

                line.write(buffer, 0, 1);
            }

            line.drain();
            line.stop();
            line.close();

        } catch (Exception e) {
            System.out.println("Could not play sound.");
        }
    }).start();
}

    private void playLockSound() {
    playSound(250, 70);
}

private void playHardDropSound() {
    new Thread(() -> {
        playSound(500, 50);
        playSound(350, 50);
        playSound(200, 100);
    }).start();
}

private void playLineClearSound() {
    new Thread(() -> {
        playSound(500, 80);
        playSound(700, 80);
        playSound(900, 120);
    }).start();
}

private void playTetrisSound() {
    new Thread(() -> {
        playSound(500, 100);
        playSound(650, 100);
        playSound(800, 100);
        playSound(1000, 180);
    }).start();
}

private void playGameOverSound() {
    new Thread(() -> {
        playSound(800, 150);
        playSound(600, 150);
        playSound(400, 200);
        playSound(200, 500);
    }).start();
}

private void playRotateSound() {
    playSound(700, 50);
}

private void playBackgroundMusic() {
    try {
        File musicFile = new File("../sound/background.wav");

        AudioInputStream audioStream =
                AudioSystem.getAudioInputStream(musicFile);

        backgroundMusic = AudioSystem.getClip();
        backgroundMusic.open(audioStream);

        backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        backgroundMusic.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void stopBackgroundMusic() {
    if (backgroundMusic != null && backgroundMusic.isRunning()) {
        backgroundMusic.stop();
        backgroundMusic.close();
    }
}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                initGame();
            }
            repaint();
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                if (!paused) moveLeft();
                break;
            case KeyEvent.VK_RIGHT:
                if (!paused) moveRight();
                break;
            case KeyEvent.VK_DOWN:
                if (!paused) moveDown();
                break;
            case KeyEvent.VK_UP:
                if (!paused) {
        rotate();
        playRotateSound();
    }
                break;
            case KeyEvent.VK_SPACE:
                if (!paused) hardDrop();
                break;
            case KeyEvent.VK_P:
                paused = !paused;
                break;
            case KeyEvent.VK_R:
                initGame();
                break;
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
