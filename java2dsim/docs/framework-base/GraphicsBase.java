

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


public class CoreFractalRenderer {

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

        private transient Image offScreenImage;
        private transient Graphics2D offScreenGraphics;



        // Frame counting and FPS tracking
        private long frameCount = 0;
        private long lastFpsTime = System.currentTimeMillis();
        private double currentFps = 0.0;
        private long fpsFrameCount = 0;

        // Modern alternative to Timer - ScheduledExecutorService for better thread management
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public BasicGraphicsCanvas() {

            // Enable keyboard input
            setFocusable(true);
            requestFocusInWindow();

            // Add keyboard listener for arrow keys
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            // Move left at current area
                            break;
                        case KeyEvent.VK_RIGHT:
                            // Move right at current area
                            break;
                        case KeyEvent.VK_UP:
                            // Move up at current area
                            break;
                        case KeyEvent.VK_DOWN:
                            // Move down at current area
                            break;
                    }

                    repaint();
                }
            });

            // Use ScheduledExecutorService instead of Timer for better control and Java 21 compatibility
            scheduler.scheduleAtFixedRate(() -> {


                System.out.println(" Print Parameters");

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
         * Render the fractal
         */
        public void renderFractalArea(final Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            //
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

            this.renderFractalArea(g2dOffscreen);

            // Render dynamic text to offscreen buffer - HUD
            g2dOffscreen.setColor(Color.black);
            g2dOffscreen.drawString("Fractal Parameters xxxx", 20, 620);
            g2dOffscreen.drawString("Total Frames: " + frameCount + " | FPS: " + String.format("%.1f", currentFps), 20, 620 + 28);
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


