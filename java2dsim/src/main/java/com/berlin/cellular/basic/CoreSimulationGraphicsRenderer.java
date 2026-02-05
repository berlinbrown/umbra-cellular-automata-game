package com.berlin.cellular.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.Random;

/**
 * Basic graphics example framework in Java Graphics 2D, render line and block dancing up and down.
 * Conway's Game of Life cellular automata with proper evolution.
 */
public class CoreSimulationGraphicsRenderer {

    public static final String TITLE = "Basic Graphics Examples";

    private int maxWidth = 800;
    private int maxHeight = 700;

    private JPanel lifeCanvas = new BasicGraphicsCanvas();

    /**
     * Launch the 2D frame window using modern Java 21 lambda syntax.
     */
    public void invokeLater() {
        SwingUtilities.invokeLater(this::launch);
    }

    /**
     * Launch the 2D frame window.
     */
    protected void launch() {
        final AutomataFrame frame = new AutomataFrame();
        frame.addWindowListener(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(20, 20);
        frame.setPreferredSize(new Dimension(maxWidth, maxHeight));
        frame.setResizable(false);
        frame.setFocusable(true);

        final JPanel panel = new JPanel();
        panel.setLocation(200, 200);
        panel.setVisible(true);
        panel.setPreferredSize(new Dimension(maxWidth, maxHeight));
        panel.setFocusable(true);
        panel.setBackground(Color.white);

        lifeCanvas.setPreferredSize(new Dimension(maxWidth, maxHeight));
        panel.add(lifeCanvas);
        // Panel setup, toggle visibility on frame, set visible
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        // Request focus AFTER window is visible
        lifeCanvas.requestFocusInWindow();
    }

    /**
     * Canvas with modern Java 21 features and Graphics2D support.
     */
    private class BasicGraphicsCanvas extends JPanel {

        private static final long serialVersionUID = 1L;

        /**
         * Entity representing an active cell in the cellular automata.
         */
        private class CellEntity {
            private final String id;
            private final int gridRow;
            private final int gridCol;
            private final int pixelX;
            private final int pixelY;

            public CellEntity(int row, int col) {
                this.gridRow = row;
                this.gridCol = col;
                this.id = "Cell_" + row + "_" + col;
                this.pixelX = gridMargin + col * cellSize;
                this.pixelY = gridMargin + row * cellSize;
            }

            public String getId() {
                return id;
            }

            public int getGridRow() {
                return gridRow;
            }

            public int getGridCol() {
                return gridCol;
            }

            public int getPixelX() {
                return pixelX;
            }

            public int getPixelY() {
                return pixelY;
            }

            public boolean intersectsPlayer(int playerX, int playerY, int playerSize) {
                return !(pixelX + cellSize < playerX ||
                        pixelX > playerX + playerSize ||
                        pixelY + cellSize < playerY ||
                        pixelY > playerY + playerSize);
            }
        }

        /**
         * Plant resource entity - static green resources scattered across the grid.
         */
        private class PlantResource {
            private final int gridX;
            private final int gridY;
            private final int pixelX;
            private final int pixelY;
            private double plantEnergy;
            private double weight;
            private final Color screenColor;

            public PlantResource(int gridX, int gridY, double plantEnergy, double weight) {
                this.gridX = gridX;
                this.gridY = gridY;
                this.pixelX = gridMargin + gridX * cellSize;
                this.pixelY = gridMargin + gridY * cellSize;
                this.plantEnergy = plantEnergy;
                this.weight = weight;
                // Forest green RGB color: (34, 139, 34)
                this.screenColor = new Color(34, 139, 34);
            }

            public int getGridX() {
                return gridX;
            }

            public int getGridY() {
                return gridY;
            }

            public int getPixelX() {
                return pixelX;
            }

            public int getPixelY() {
                return pixelY;
            }

            public double getPlantEnergy() {
                return plantEnergy;
            }

            public void setPlantEnergy(double energy) {
                this.plantEnergy = energy;
            }

            public double getWeight() {
                return weight;
            }

            public void setWeight(double w) {
                this.weight = w;
            }

            public Color getScreenColor() {
                return screenColor;
            }
        }

        private transient Image offScreenImage;
        private transient Graphics2D offScreenGraphics;

        // Cellular automata grid
        private final int gridMargin = 140;
        private final int cellSize = 8;
        private int gridRows;
        private int gridCols;
        private boolean[][] cells;
        private boolean[][] nextCells;
        private java.util.List<CellEntity> activeEntities = new java.util.ArrayList<>();

        // Plant resources
        private java.util.List<PlantResource> plantResources = new java.util.ArrayList<>();

        // Cell entity tracking
        private int cellEntityCount = 0;

        private int randY = 160;
        private int randY2Box = 120;

        // Player box position
        private int playerX = 100;
        private int playerY = 100;
        private final int playerSize = 20;
        private final int playerSpeed = 10;

        // Observer entity (fits one cell in the cellular grid)
        private int observerRow = 0;
        private int observerCol = 0;
        // Direction: 1 = moving right, -1 = moving left
        private int observerDir = 1;
        // Food resource counter collected by observer
        private double foodResources = 0.0;

        // Frame counting and FPS tracking
        private long frameCount = 0;
        private long lastFpsTime = System.currentTimeMillis();
        private double currentFps = 0.0;
        private long fpsFrameCount = 0;

        // Modern alternative to Timer - ScheduledExecutorService for better thread management
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public BasicGraphicsCanvas() {
            // Initialize cellular automata grid
            gridRows = (maxHeight - 2 * gridMargin) / cellSize;
            gridCols = (maxWidth - 2 * gridMargin) / cellSize;
            cells = new boolean[gridRows][gridCols];
            nextCells = new boolean[gridRows][gridCols];

            // Initialize observer to middle row so it's visible immediately
            observerRow = gridRows / 2;
            observerCol = 0;
            observerDir = 1;

            // Initialize with random pattern
            Random random = new Random();
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    cells[row][col] = random.nextDouble() < 0.15; // 15% chance of being alive
                }
            }

            // Initialize plant resources randomly across the grid
            initializePlantResources(random);

            // Enable keyboard input
            setFocusable(true);
            requestFocusInWindow();

            // Add keyboard listener for arrow keys
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            playerX -= playerSpeed;
                            break;
                        case KeyEvent.VK_RIGHT:
                            playerX += playerSpeed;
                            break;
                        case KeyEvent.VK_UP:
                            playerY -= playerSpeed;
                            break;
                        case KeyEvent.VK_DOWN:
                            playerY += playerSpeed;
                            break;
                    }
                    // Keep player within bounds
                    playerX = Math.max(0, Math.min(playerX, maxWidth - playerSize));
                    playerY = Math.max(0, Math.min(playerY, maxHeight - playerSize));
                    repaint();
                }
            });

            // Use ScheduledExecutorService instead of Timer for better control and Java 21 compatibility
            scheduler.scheduleAtFixedRate(() -> {

                // Update cellular automata using Conway's Game of Life rules
                updateCellularAutomata();

                // For each update, change Y to random position
                randY = random.nextInt(280);
                randY2Box = random.nextInt(280);
                // Advance observer position horizontally within grid bounds
                updateObserverPosition();
                // Check for interaction with alive cell at observer position
                if (observerRow >= 0 && observerRow < gridRows && observerCol >= 0 && observerCol < gridCols) {
                    if (cells[observerRow][observerCol]) {
                        // Collect food and consume the cell so it's not repeatedly counted
                        foodResources += 0.5;
                        cells[observerRow][observerCol] = false;
                            // Remove matching active entity immediately so HUD and collision reflect change
                            activeEntities.removeIf(e -> e.getGridRow() == observerRow && e.getGridCol() == observerCol);
                            System.out.println("Observer collected food at (" + observerRow + "," + observerCol + ") total=" + foodResources);
                    }
                }
                System.out.println("Random Y position at " + randY + " // " + randY2Box + " // playerX " + playerX);

                if (!EventQueue.isDispatchThread()) {
                    EventQueue.invokeLater(() -> {
                        if (lifeCanvas != null) {
                            lifeCanvas.repaint();
                        }
                    });
                } else {
                    if (lifeCanvas != null) {
                        lifeCanvas.repaint();
                    }
                }
            }, 0, 200, TimeUnit.MILLISECONDS);
        }

        /**
         * Initialize plant resources randomly scattered across the grid.
         */
        private void initializePlantResources(Random random) {
            plantResources.clear();
            // Distribute plants across approximately 10-15% of grid cells
            int plantCount = (int) (gridRows * gridCols * 0.12);
            for (int i = 0; i < plantCount; i++) {
                int randomGridX = random.nextInt(gridCols);
                int randomGridY = random.nextInt(gridRows);
                double plantEnergy = 50.0 + random.nextDouble() * 50.0; // Energy between 50-100
                double weight = 5.0 + random.nextDouble() * 5.0; // Weight between 5-10
                plantResources.add(new PlantResource(randomGridX, randomGridY, plantEnergy, weight));
            }
            System.out.println("Initialized " + plantResources.size() + " plant resources");
        }

        /**
         * Advance observer horizontally and bounce at grid edges.
         */
        private void updateObserverPosition() {
            // Ensure observerRow is set (init after gridRows available)
            if (observerRow == 0) {
                observerRow = gridRows / 2; // middle row by default
                observerCol = 0;
                observerDir = 1;
            }

            observerCol += observerDir;
            if (observerCol >= (gridCols - 1)) {
                observerCol = gridCols - 1;
                observerDir = -1;
            } else if (observerCol <= 0) {
                observerCol = 0;
                observerDir = 1;
            }
        }

        /**
         * Render the cell grid using Graphics2D for enhanced rendering.
         */
        public void renderLines(final Graphics2D g2d) {

            // Enable anti-aliasing for smoother lines
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Use Graphics2D geometric shapes for better precision
            g2d.setColor(Color.black);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(new Line2D.Double(30, 10, 80, randY));
        }

        /**
         * Render all plant resources on the grid.
         */
        public void renderPlantResources(final Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            for (PlantResource plant : plantResources) {
                // Fill cell with forest green
                g2d.setColor(plant.getScreenColor());
                g2d.fillRect(plant.getPixelX() + 1, plant.getPixelY() + 1, cellSize - 1, cellSize - 1);

                // Optional: Add a subtle border
                g2d.setColor(new Color(20, 100, 20)); // Darker green border
                g2d.setStroke(new BasicStroke(0.5f));
                g2d.drawRect(plant.getPixelX() + 1, plant.getPixelY() + 1, cellSize - 1, cellSize - 1);
            }
        }

        /**
         * Render all cells if alive using Graphics2D.
         */
        public void renderBox(final Graphics2D g2d) {
            // Use Graphics2D for enhanced rendering with geometric shapes
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.blue);
            g2d.fill(new Rectangle2D.Double(200, 150, 120, randY2Box));
        }

        /**
         * Render the player-controlled green box.
         */
        public void renderPlayer(final Graphics2D g2d) {
            // Filled player box
            g2d.setColor(Color.darkGray);
            g2d.fill(new Rectangle2D.Double(playerX, playerY, playerSize, playerSize));

            // Black outline around the player box so it stands out
            g2d.setColor(Color.red);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(new Rectangle2D.Double(playerX, playerY, playerSize, playerSize));

            // Draw a short line anchored to the player's right-middle so the line
            // moves together with the player. Use `randY` to vary vertical offset.
            final double startX = playerX + playerSize;
            final double startY = playerY + (playerSize / 2.0);
            final double endX = playerX + playerSize + 60;
            final double endY = playerY + (playerSize / 2.0) + ((randY % 40) - 20);
            g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        /**
         * Update cellular automata using Conway's Game of Life rules.
         */
        private void updateCellularAutomata() {
            // Calculate next generation
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    int neighbors = countNeighbors(row, col);

                    // Conway's Game of Life rules:
                    // 1. Any live cell with 2-3 neighbors survives
                    // 2. Any dead cell with exactly 3 neighbors becomes alive
                    // 3. All other cells die or stay dead
                    if (cells[row][col]) {
                        nextCells[row][col] = (neighbors == 2 || neighbors == 3);
                    } else {
                        nextCells[row][col] = (neighbors == 3);
                    }
                }
            }

            // Swap arrays
            boolean[][] temp = cells;
            cells = nextCells;
            nextCells = temp;

            // Rebuild active entity list
            activeEntities.clear();
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    if (cells[row][col]) {
                        activeEntities.add(new CellEntity(row, col));
                    }
                }
            }
        }

        /**
         * Count live neighbors for a cell.
         */
        private int countNeighbors(int row, int col) {
            int count = 0;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;

                    int r = row + dr;
                    int c = col + dc;

                    // Wrap around edges (toroidal grid)
                    if (r < 0) r = gridRows - 1;
                    if (r >= gridRows) r = 0;
                    if (c < 0) c = gridCols - 1;
                    if (c >= gridCols) c = 0;

                    if (cells[r][c]) count++;
                }
            }
            return count;
        }

        /**
         * Render the cellular automata grid with light grey lines and alive cells.
         */
        public void renderCellularAutomata(final Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            // Draw grid lines (light grey)
            g2d.setColor(new Color(220, 220, 220));
            g2d.setStroke(new BasicStroke(1.0f));

            // Vertical lines
            for (int col = 0; col <= gridCols; col++) {
                int x = gridMargin + col * cellSize;
                g2d.drawLine(x, gridMargin, x, gridMargin + gridRows * cellSize);
            }

            // Horizontal lines
            for (int row = 0; row <= gridRows; row++) {
                int y = gridMargin + row * cellSize;
                g2d.drawLine(gridMargin, y, gridMargin + gridCols * cellSize, y);
            }

            // Draw alive cells (filled rectangles)
            g2d.setColor(new Color(50, 50, 200));
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    if (cells[row][col]) {
                        int x = gridMargin + col * cellSize;
                        int y = gridMargin + row * cellSize;
                        g2d.fillRect(x + 1, y + 1, cellSize - 1, cellSize - 1);
                    }
                }
            }
        }

        /**
         * Use double buffering with Graphics2D.
         *
         * @see java.awt.Component#update(java.awt.Graphics)
         */
        @Override
        public void update(final Graphics g) {
            final Graphics2D g2d = (Graphics2D) g;
            final Dimension d = getSize();
            if (offScreenImage == null) {
                offScreenImage = createImage(d.width, d.height);
                offScreenGraphics = (Graphics2D) offScreenImage.getGraphics();
                // Configure Graphics2D rendering hints
                offScreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                offScreenGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            // Render to off screen graphics buffer
            this.paint(offScreenGraphics);

            // Flip buffer
            g2d.drawImage(offScreenImage, 0, 0, null);
        }

        /**
         * Draw this generation with Graphics2D for enhanced rendering.
         * All rendering goes to offScreenImage, then flips to screen in update().
         *
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        @Override
        public void paint(final Graphics g) {
            // g is offScreenGraphics passed from update() - NOT the actual screen!
            final Graphics2D g2dOffscreen = (Graphics2D) g;
            final Dimension d = getSize();

            // Update frame count and calculate FPS
            frameCount++;
            fpsFrameCount++;
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastFpsTime;

            // Update FPS every second
            if (timeDiff >= 1000) {
                currentFps = (fpsFrameCount * 1000.0) / timeDiff;
                fpsFrameCount = 0;
                lastFpsTime = currentTime;
            }

            // Enable anti-aliasing for all rendering to offscreen buffer
            g2dOffscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2dOffscreen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2dOffscreen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Clear background - render to offscreen buffer
            g2dOffscreen.setColor(Color.white);
            g2dOffscreen.fill(new Rectangle2D.Double(0, 0, d.width, d.height));

            // Render cellular automata grid and cells
            this.renderCellularAutomata(g2dOffscreen);

            // Render plant resources
            this.renderPlantResources(g2dOffscreen);

            // Render observer (one-cell entity that moves horizontally)
            this.renderObserver(g2dOffscreen);

            // Render all dynamic content to offscreen buffer
            this.renderPlayer(g2dOffscreen);

            // Render random boxes on top of the grid
            this.renderBox(g2dOffscreen);
            this.renderLines(g2dOffscreen);

            // Check for collision with active cells and display info
            g2dOffscreen.setColor(Color.red);
            int textY = 30;
            for (CellEntity entity : activeEntities) {
                if (entity.intersectsPlayer(playerX, playerY, playerSize)) {
                    g2dOffscreen.drawString("Overlapping: " + entity.getId() +
                                    " at grid(" + entity.getGridRow() + "," + entity.getGridCol() + ")" +
                                    " pixel(" + entity.getPixelX() + "," + entity.getPixelY() + ")",
                            gridMargin, textY);
                    textY += 15;
                }
            }

            // Calculate total plant energy
            double totalPlantEnergy = plantResources.stream()
                .mapToDouble(PlantResource::getPlantEnergy)
                .sum();

            // Update cell entity count
            cellEntityCount = activeEntities.size();

            // Render dynamic text to offscreen buffer
            g2dOffscreen.setColor(Color.black);
            g2dOffscreen.drawString("Active Cells: " + cellEntityCount, 20, 590);
            g2dOffscreen.drawString("Plants: " + plantResources.size() + " | Total Energy: " + String.format("%.1f", totalPlantEnergy), 20, 605);
            g2dOffscreen.drawString("Umbra 2D Life Simulation - " + gridRows + "x" + gridCols + " cells", 20, 620);
            g2dOffscreen.drawString("Player: (" + playerX + ", " + playerY + ") - Use Arrow Keys", 20, 620 + 14);
            g2dOffscreen.drawString("Total Frames: " + frameCount + " | FPS: " + String.format("%.1f", currentFps), 20, 620 + 28);
            g2dOffscreen.drawString("Observer Food: " + String.format("%.1f", foodResources), 20, 620 + 42);
        }

        /**
         * Render a dark blue observer that occupies one grid cell and moves horizontally.
         */
        public void renderObserver(final Graphics2D g2d) {
            if (gridRows <= 0 || gridCols <= 0) return;

            // Calculate pixel position aligned to the grid
            final int x = gridMargin + observerCol * cellSize;
            final int y = gridMargin + observerRow * cellSize;

            // Filled dark blue cell (same size as cellular automata cells)
            g2d.setColor(new Color(0, 0, 220));
            g2d.fillRect(x + 1, y + 1, cellSize - 1, cellSize - 1);

            // Thin black outline so it stands out
            g2d.setColor(Color.black);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRect(x + 1, y + 1, cellSize - 1, cellSize - 1);
        }

    } // End of the class //

    /**
     * JFrame with window listener.
     */
    private class AutomataFrame extends JFrame implements WindowListener {
        private static final long serialVersionUID = 1L;

        public AutomataFrame() {
            super(TITLE);
        }

        @Override
        public void windowOpened(WindowEvent e) {
            System.out.println("Frame Window Opened");
        }

        @Override
        public void windowClosing(WindowEvent e) {
            System.out.println("Frame Window Closing");
        }

        @Override
        public void windowClosed(WindowEvent e) {
            System.out.println("Frame Window Closed");
        }

        @Override
        public void windowIconified(WindowEvent e) {
            System.out.println("Frame Window Minimized");
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            System.out.println("Frame Window Maximized");
        }

        @Override
        public void windowActivated(WindowEvent e) {
            System.out.println("Frame Window Activated");
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            System.out.println("Frame Window Deactivated");
        }
    } // End of the Class //
}


