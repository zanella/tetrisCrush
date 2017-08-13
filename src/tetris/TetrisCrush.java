package tetris;

import com.google.common.collect.EvictingQueue;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.*;

import static java.lang.Math.abs;
import static tetris.Tetraminos.*;

public class TetrisCrush extends JPanel {
    private JFrame f;
    private Tetramino currentPiece;

    private long score;
    private Color[][] well;

    private final static int WIDTH = 325, HEIGHT = 650, SQUARE_SIDE_SIZE = 25;

    private final int COLUMNS, ROWS;

    private void init() { // Creates a border around the well and initializes the dropping piece
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

        f.add(this);

        setupGameLoop();
    }

    private void newPiece() { // Puts a new, random piece into the dropping position
        currentPiece = Tetraminos.random();
    }

    private boolean collidesAt(int x, int y) { // Collision test for the dropping piece
        for (Point p : currentPiece.points.get(currentPiece.rotation)) {
            if (well[p.x + x][p.y + y] != Color.BLACK) {
                return true;
            }
        }
        return false;
    }

    private void rotate(int i) { // Rotates the piece clockwise or counterclockwise
        int newRotation = (currentPiece.rotation + i) % 4;
        if (newRotation < 0) {
            newRotation = 3;
        }
        if (!collidesAt(currentPiece.x, currentPiece.y)) {
            currentPiece.rotation = newRotation;
        }
        repaint();
    }

    private void movePiece(int i) { // Moves the piece horizontally
        if (!collidesAt(currentPiece.x + i, currentPiece.y)) {
            currentPiece.x += i;
        }

        repaint();
    }

    private boolean dropDown() { // Drops the piece one line or fixes it to the well if it can't drop
        final boolean collisionCheck = collidesAt(currentPiece.x, currentPiece.y + 1);

        if (!collisionCheck) {
            currentPiece.y += 1;
        } else {
            fixToWell();
        }

        repaint();

        return collisionCheck;
    }

    private void dropInstant() { // Drops until it can't, instantaneously
        while ( !dropDown() ) { score++; }
    }

    /**
     * Make the dropping piece part of the well, so it is available for collision detection.
     *
     * It's actually pretty ingenious, it doesn't need to redraw the squares in the "well"
     */
    private void fixToWell() {
        for (int i = 0; i < currentPiece.points.get(currentPiece.rotation).size(); i++) {
            final Point p = currentPiece.points.get(currentPiece.rotation).get(i);

            well[currentPiece.x + p.x][currentPiece.y + p.y] = currentPiece.pointsColor.get(i);
        }

        clearRows();

        newPiece();
    }

    /**
     * Shifts down the rows "above" this one
     *
     * @param row index of row with no gaps
     */
    private void deleteRow(int row) {
        for (int j = row-1; j > 0; j--) {
            for (int i = 1; i < COLUMNS; i++) {
                well[i][j+1] = well[i][j];
            }
        }
    }

    // Clear completed rows from the field and award score according to
    // the number of simultaneously cleared rows.
    private void clearRows() {
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

    private void drawPiece(Graphics g) { // Draw the falling piece
        for (int i = 0; i < currentPiece.pointsColor.size(); i++) {

            final Color c = currentPiece.pointsColor.get(i);
            g.setColor( currentPiece.pointsColor.get(i) );
            //System.out.println("color["+i+"]: " + c);

            final Point p = currentPiece.points.get(currentPiece.rotation).get(i);
            final int x = p.x + currentPiece.x;
            final int y = p.y + currentPiece.y;

            g.fill3DRect((x) * (SQUARE_SIDE_SIZE), (y) * (SQUARE_SIDE_SIZE),
                        SQUARE_SIDE_SIZE, SQUARE_SIDE_SIZE, true);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    private final EvictingQueue<Point> chosenSquares = EvictingQueue.create(2);

    private void drawHighlighted(final Graphics g) {
        for (Integer i = chosenSquares.size(); i > 0 ; i--) {
            final Point p = chosenSquares.remove();

            g.setColor(well[p.x][p.y]);
            g.draw3DRect(SQUARE_SIDE_SIZE * p.x, SQUARE_SIDE_SIZE * p.y,
                    SQUARE_SIDE_SIZE, SQUARE_SIDE_SIZE, true);

            System.out.println("Color highlighted: " + well[p.x][p.y]);

            g.setColor(Color.WHITE);
            g.drawString(i.toString(),
                    (SQUARE_SIDE_SIZE * p.x) + 12, (SQUARE_SIDE_SIZE * p.y) + 12);

            chosenSquares.add(p);
        }
    }

    private void highlightSquare(int mouseX, int mouseY) {
        final Point p = new Point((mouseX / SQUARE_SIDE_SIZE), (mouseY / SQUARE_SIDE_SIZE));
        final Color pc = well[p.x][p.y];
        System.out.println("mouse_x: " + mouseX + ", mouse_y: " + mouseY + " color: " + pc);
        if ( Color.BLACK.equals(pc) ) { return; }

        chosenSquares.add(p);

        if (chosenSquares.size() == 2) {
            final Point a = chosenSquares.remove();
            final Point b = chosenSquares.remove();

            final int horizontalAndVerticalDist = abs(a.x - b.x) + abs(a.y - b.y);
            if (horizontalAndVerticalDist == 1) {
                final Color aColor = well[a.x][a.y];
                final Color bColor = well[b.x][b.y];

                // TODO - animate switch
                well[a.x][a.y] = bColor;
                well[b.x][b.y] = aColor;

                if ( !clearMatches(a, aColor, b, bColor) ) {
                    well[a.x][a.y] = aColor;
                    well[b.x][b.y] = bColor;
                }
            }
        }

        repaint();
    }

    private boolean clearMatches(Point a, Color aColor, Point b, Color bColor) {
        return clearMatches(a, bColor) || clearMatches(b, aColor);
    }

    private boolean clearMatches(Point b, Color aColor) {
        final java.util.List<Point> xStretch = new LinkedList<>( Collections.singletonList(b));

        { // ROW
            for (int x = b.x + 1; x < COLUMNS; x++) {
                if (well[x][b.y].equals(aColor)) {
                    xStretch.add(new Point(x, b.y));
                } else {
                    break;
                }
            }

            for (int x = b.x - 1; x > 0; x--) {
                if ( well[x][b.y].equals(aColor) ) {
                    xStretch.add( new Point(x, b.y) );
                } else {
                    break;
                }
            }
        }
        System.out.print("xStretch: "); xStretch.forEach(System.out::print); System.out.println();

        final List<Point> yStretch = new LinkedList<>( Collections.singletonList(b) );

        { // COLUMN
            for (int y = b.y + 1; y < ROWS; y++) {
                if (well[b.x][y].equals(aColor)) {
                    yStretch.add(new Point(b.x, y));
                } else {
                    break;
                }
            }

            for (int y = b.y - 1; y > 0 ; y--) {
                if ( well[b.x][y].equals(aColor) ) {
                    yStretch.add( new Point(b.x, y) );
                } else {
                    break;
                }
            }
        }
        System.out.print("yStretch: "); yStretch.forEach(System.out::print); System.out.println();

        boolean ret = false;
        if (xStretch.size() >= 3) {
            xStretch.forEach(p -> well[p.x][p.y] = Color.BLACK);
            ret = true;
        }

        if (yStretch.size() >= 3) {
            yStretch.forEach(p -> well[p.x][p.y] = Color.BLACK);
            ret = true;
        }

        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void paintComponent(Graphics g) {
        { // Paint the well
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < COLUMNS; i++) {
                for (int j = 0; j < ROWS; j++) {
                    g.setColor(well[i][j]);
                    g.fill3DRect((SQUARE_SIDE_SIZE) * i, (SQUARE_SIDE_SIZE) * j,
                            SQUARE_SIDE_SIZE, SQUARE_SIDE_SIZE, true);
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

        drawHighlighted(g);
    }

    private void addKeyboardListeners() {
        f.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    rotate(-1);                 break;
                    case KeyEvent.VK_DOWN:  dropDown();     score += 1;   break;
                    case KeyEvent.VK_LEFT:  movePiece(-1);              break;
                    case KeyEvent.VK_RIGHT: movePiece(+1);              break;
                    case KeyEvent.VK_SPACE: dropInstant();                break;
                    case KeyEvent.VK_P:     PAUSE = !PAUSE;               break;
                    case KeyEvent.VK_ESCAPE:    System.exit(0);
                }
            }

            public void keyReleased(KeyEvent e) {}
        });
    }

    private void addMouseListeners() {
        f.addMouseListener(new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {
                highlightSquare(e.getX(), e.getY());
            }
        });
    }

    private boolean PAUSE = false;

    private void setupGameLoop() {
        addKeyboardListeners();

        addMouseListeners();

        new Thread(() -> { // Make the falling piece drop every second
            while (true) {
                try {
                    Thread.sleep(1000);

                    if ( !PAUSE ) { dropDown(); }
                } catch ( InterruptedException e ) {
                    System.exit(1);
                }
            }
        }).start();
    }

    private TetrisCrush() {
        f = new JFrame("TetrisCrush");

        final Dimension dimension = new Dimension(WIDTH, HEIGHT);

        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setMinimumSize(dimension);
        f.setVisible(true);

        COLUMNS = dimension.width / SQUARE_SIDE_SIZE;

        // -1 hides the sum of drawing "errors"
        ROWS = (dimension.height / SQUARE_SIDE_SIZE);// - 1;
    }

    public static void main(String[] args) {
        final TetrisCrush tc = new TetrisCrush();

        tc.init();
    }
}