import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Basic mandelbrot fractal in java 2d format
 */
public class BasicFractalRenderer {

    public static final String TITLE = "Basic Fractal Renderer";

    private final int maxWidth = 800;
    private final int maxHeight = 700;

    private final JPanel canvas = new FractalCanvas();

    public void invokeLater() {
        SwingUtilities.invokeLater(this::launch);
    }

    private void launch() {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(maxWidth, maxHeight));
        frame.setResizable(false);

        canvas.setPreferredSize(new Dimension(maxWidth, maxHeight));
        frame.add(canvas);

        frame.pack();
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    private static final class FractalCanvas extends JPanel {

        private Image offscreen;
        private Graphics2D offG;

        // ---- Default Mandelbrot view ----
        private static final double DEFAULT_MIN_X = -2.5;
        private static final double DEFAULT_MAX_X = 1.2;
        private static final double DEFAULT_MIN_Y = -1.5;
        private static final double DEFAULT_MAX_Y = 1.5;

        // ---- Current view ----
        private double minX = DEFAULT_MIN_X;
        private double maxX = DEFAULT_MAX_X;
        private double minY = DEFAULT_MIN_Y;
        private double maxY = DEFAULT_MAX_Y;

        private static final int MAX_ITER = 350;

        // ---- Layout ----
        private static final int MARGIN_TOP = 10;
        private static final int MARGIN_BOTTOM = 90;
        private static final int MARGIN_LEFT = 10;
        private static final int MARGIN_RIGHT = 10;

        // ---- Mouse zoom ----
        private Point dragStart;
        private Point dragEnd;

        // ---- FPS ----
        private long frames;
        private long lastTime = System.currentTimeMillis();
        private double fps;

        private final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        FractalCanvas() {
            setFocusable(true);

            // ---- Keyboard controls ----
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {

                    double dx = (maxX - minX) * 0.08;
                    double dy = (maxY - minY) * 0.08;

                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT -> {
                            minX -= dx;
                            maxX -= dx;
                        }
                        case KeyEvent.VK_RIGHT -> {
                            minX += dx;
                            maxX += dx;
                        }
                        case KeyEvent.VK_UP -> {
                            minY -= dy;
                            maxY -= dy;
                        }
                        case KeyEvent.VK_DOWN -> {
                            minY += dy;
                            maxY += dy;
                        }

                        case KeyEvent.VK_G -> resetView();
                    }
                    repaint();
                }
            });

            // ---- Mouse zoom ----
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        dragStart = e.getPoint();
                        dragEnd = null;
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        zoomOut();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragStart != null && dragEnd != null) {
                        zoomToRectangle();
                        dragStart = null;
                        dragEnd = null;
                        repaint();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    dragEnd = e.getPoint();
                    repaint();
                }
            });

            scheduler.scheduleAtFixedRate(
                    () -> EventQueue.invokeLater(this::repaint),
                    0,
                    200,
                    TimeUnit.MILLISECONDS
            );
        }

        private void resetView() {
            minX = DEFAULT_MIN_X;
            maxX = DEFAULT_MAX_X;
            minY = DEFAULT_MIN_Y;
            maxY = DEFAULT_MAX_Y;
        }

        private void zoomOut() {
            double cx = (minX + maxX) / 2.0;
            double cy = (minY + maxY) / 2.0;

            double scale = 1.6;
            double w = (maxX - minX) * scale;
            double h = (maxY - minY) * scale;

            minX = cx - w / 2;
            maxX = cx + w / 2;
            minY = cy - h / 2;
            maxY = cy + h / 2;
        }

        private void zoomToRectangle() {
            int w = getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
            int h = getHeight() - MARGIN_TOP - MARGIN_BOTTOM;

            int x1 = Math.min(dragStart.x, dragEnd.x) - MARGIN_LEFT;
            int x2 = Math.max(dragStart.x, dragEnd.x) - MARGIN_LEFT;
            int y1 = Math.min(dragStart.y, dragEnd.y) - MARGIN_TOP;
            int y2 = Math.max(dragStart.y, dragEnd.y) - MARGIN_TOP;

            double nx1 = minX + (x1 / (double) w) * (maxX - minX);
            double nx2 = minX + (x2 / (double) w) * (maxX - minX);
            double ny1 = minY + (y1 / (double) h) * (maxY - minY);
            double ny2 = minY + (y2 / (double) h) * (maxY - minY);

            minX = nx1;
            maxX = nx2;
            minY = ny1;
            maxY = ny2;
        }

        private void renderFractal(Graphics2D g) {
            int width = getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
            int height = getHeight() - MARGIN_TOP - MARGIN_BOTTOM;

            BufferedImage img =
                    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int px = 0; px < width; px++) {
                for (int py = 0; py < height; py++) {

                    double x0 = minX + px * (maxX - minX) / width;
                    double y0 = minY + py * (maxY - minY) / height;

                    double x = 0.0;
                    double y = 0.0;
                    int iter = 0;

                    while (x * x + y * y <= 4 && iter < MAX_ITER) {
                        double xt = x * x - y * y + x0;
                        y = 2 * x * y + y0;
                        x = xt;
                        iter++;
                    }

                    int rgb = (iter == MAX_ITER)
                            ? 0x000000
                            : Color.HSBtoRGB(iter / 256f, 0.9f,
                            iter / (iter + 6f));

                    img.setRGB(px, py, rgb);
                }
            }

            g.drawImage(img, MARGIN_LEFT, MARGIN_TOP, null);

            if (dragStart != null && dragEnd != null) {
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(2));
                g.drawRect(
                        Math.min(dragStart.x, dragEnd.x),
                        Math.min(dragStart.y, dragEnd.y),
                        Math.abs(dragStart.x - dragEnd.x),
                        Math.abs(dragStart.y - dragEnd.y)
                );
            }
        }

        @Override
        public void update(Graphics g) {
            if (offscreen == null) {
                offscreen = createImage(getWidth(), getHeight());
                offG = (Graphics2D) offscreen.getGraphics();
                offG.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
            }
            paint(offG);
            g.drawImage(offscreen, 0, 0, null);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;

            frames++;
            long now = System.currentTimeMillis();
            if (now - lastTime >= 1000) {
                fps = frames * 1000.0 / (now - lastTime);
                frames = 0;
                lastTime = now;
            }

            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));

            renderFractal(g2d);

            g2d.setColor(Color.BLACK);
            int y = getHeight() - 60;

            g2d.drawString(
                    String.format("X: [%.6f , %.6f]", minX, maxX),
                    20, y);
            g2d.drawString(
                    String.format("Y: [%.6f , %.6f]", minY, maxY),
                    20, y + 16);
            g2d.drawString(
                    String.format("ΔX: %.6f  ΔY: %.6f",
                            (maxX - minX), (maxY - minY)),
                    20, y + 32);
            g2d.drawString(
                    "FPS: " + String.format("%.1f", fps)
                            + " | Arrows=Pan  Drag=Zoom  RightClick=Out  G=Reset",
                    20, y + 48);
        }
    }

    public static void main(String[] args) {
        new BasicFractalRenderer().invokeLater();
    }
}