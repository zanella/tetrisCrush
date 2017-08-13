package tetris;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;

import static tetris.Tetraminos.*;

public class Tetris extends JPanel {
    private Point pieceOrigin;
    private int currentPiece;
    private int rotation;
    private ArrayList<Integer> nextPieces = new ArrayList<>();

    private long score;
    private Color[][] well;

    private final static int WIDTH = 320, HEIGHT = 640, SQUARE_SIDE_SIZE = 25;

    private final int COLUMNS, ROWS;

    private Tetris(final Dimension dimension) {
        COLUMNS = dimension.width / SQUARE_SIDE_SIZE;
        // -1 accounts for the sum of drawing "errors"
        ROWS = (dimension.height / SQUARE_SIDE_SIZE) - 1;
    }

    // Creates a border around the well and initializes the dropping piece
    private void init() {
        well = new Color[COLUMNS][ROWS];

        for (int i = 0; i < COLUMNS; i++) {
            for (int j = 0; j < ROWS; j++) {
                if (i == 0 || i == (COLUMNS - 1) || j == (ROWS - 1)) {
                    well[i][j] = Color.GRAY;
                } else {
                    well[i][j] = Color.BLACK;
                }
            }
        }

        newPiece();
    }

    private void newPiece() {
        pieceOrigin = new Point(5, 0);

        rotation = 0;

        if (nextPieces.isEmpty()) {
            Collections.addAll(nextPieces, 0, 1, 2, 3, 4, 5, 6);
            Collections.shuffle(nextPieces);
        }
        currentPiece = nextPieces.remove(0);
    }

    // Collision test for the dropping piece
    private boolean collidesAt(int x, int y, int rotation) {
        for (Point p : PIECES[currentPiece][rotation]) {
            if (well[p.x + x][p.y + y] != Color.BLACK) {
                return true;
            }
        }
        return false;
    }

    private void rotate(int i) { // Rotates the piece clockwise or counterclockwise
        int newRotation = (rotation + i) % 4;
        if (newRotation < 0) {
            newRotation = 3;
        }
        if (!collidesAt(pieceOrigin.x, pieceOrigin.y, newRotation)) {
            rotation = newRotation;
        }
        repaint();
    }

    private void move(int i) { // Moves the piece horizontally
        if (!collidesAt(pieceOrigin.x + i, pieceOrigin.y, rotation)) {
            pieceOrigin.x += i;
        }
        repaint();
    }

    private boolean dropDown() { // Drops the piece one line or fixes it to the well if it can't drop
        final boolean collisionCheck = collidesAt(pieceOrigin.x, pieceOrigin.y + 1, rotation);

        if (!collisionCheck) {
            pieceOrigin.y += 1;
        } else {
            fixToWell();
        }

        repaint();

        return collisionCheck;
    }

    private void dropInstant() { // Drops until it can't, instantaneously
        while ( !dropDown() ) { ; }
    }

    /**
     * Make the dropping piece part of the well, so it is available for collision detection.
     *
     * It's actually pretty ingenious, it doesn't need to redraw the squares in the "well"
     */
    private void fixToWell() {
        for (Point p : PIECES[currentPiece][rotation]) {
            well[pieceOrigin.x + p.x][pieceOrigin.y + p.y] = COLORS[currentPiece];
        }

        clearRows();

        newPiece();
    }

    /**
     * Shifts down the rows "above" this one
     *
     * @param row index of row with no gaps
     */
    public void deleteRow(int row) {
        for (int j = row-1; j > 0; j--) {
            for (int i = 1; i < COLUMNS; i++) {
                well[i][j+1] = well[i][j];
            }
        }
    }

    // Clear completed rows from the field and award score according to
    // the number of simultaneously cleared rows.
    public void clearRows() {
        boolean gap;
        int numClears = 0;

        // -2 = (0-offset array) && "bottom/limit" row
        for (int j = (ROWS - 2); j > 0; j--) {
            gap = false;
            for (int i = 1; i < COLUMNS; i++) {
                if (well[i][j] == Color.BLACK) {
                    gap = true;
                    break;
                }
            }

            if (!gap) {
                deleteRow(j);
                j += 1;
                numClears += 1;
            }
        }

        switch (numClears) {
            case 1:
                score += 100;
                break;
            case 2:
                score += 300;
                break;
            case 3:
                score += 500;
                break;
            case 4:
                score += 800;
                break;
        }
    }

    // Draw the falling piece
    private void drawPiece(Graphics g) {
        g.setColor(COLORS[currentPiece]);
        for (Point p : Tetraminos.PIECES[currentPiece][rotation]) {
            g.fillRect((p.x + pieceOrigin.x) * (SQUARE_SIDE_SIZE + 1),
                    (p.y + pieceOrigin.y) * (SQUARE_SIDE_SIZE + 1),
                    25, 25);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        { // Paint the well
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < COLUMNS; i++) {
                for (int j = 0; j < ROWS; j++) {
                    g.setColor(well[i][j]);
                    g.fillRect((SQUARE_SIDE_SIZE + 1) * i, (SQUARE_SIDE_SIZE + 1) * j,
                            SQUARE_SIDE_SIZE, SQUARE_SIDE_SIZE);
                }
            }
        }

        { // Display the score
            g.setColor(Color.WHITE);
            g.drawString("SCORE: " + score, 19 * 12, 25);
        }

        { // Draw the currently falling piece
            drawPiece(g);
        }
    }

    private static void gameLoop(final JFrame f, final Tetris game) {
        // Keyboard controls
        f.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        game.rotate(-1);
                        break;
                    case KeyEvent.VK_DOWN:
                        game.dropDown();
                        game.score += 1;
                        break;
                    case KeyEvent.VK_LEFT:
                        game.move(-1);
                        break;
                    case KeyEvent.VK_RIGHT:
                        game.move(+1);
                        break;
                    case KeyEvent.VK_SPACE:
                        game.dropInstant();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        System.exit(0);
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });

        // Make the falling piece drop every second
        new Thread() {
            @Override public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        game.dropDown();
                    } catch ( InterruptedException e ) {}
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        final JFrame f = new JFrame("tetris.Tetris");

        final Dimension dimension = new Dimension(WIDTH, HEIGHT);

        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setMinimumSize(dimension);
        f.setVisible(true);

        final Tetris game = new Tetris(dimension);
        game.init();
        f.add(game);

        gameLoop(f, game);
    }
}