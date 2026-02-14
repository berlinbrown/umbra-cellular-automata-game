package org.berlin.termsim;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.util.Random;

/**
 * Terminal Simulator Basic, foundation.
 */
public class TermSimGameLifeApp {

    static final int CMD_HEIGHT = 7;

    enum Mode {
        NORMAL, COMMAND
    }

    private int width = 80;
    private int height = 25;
    private int gridHeight = 18;

    private Mode mode = Mode.NORMAL;
    private int cursorX = 0;
    private int cursorY = 0;
    private StringBuilder commandBuffer = new StringBuilder();

    private boolean[][] grid;
    private boolean[][] nextGrid;
    private final Random random = new Random();

    private Screen screen;

    public static void main(String[] args) throws Exception {
        new TermSimGameLifeApp().start();
    }

    void start() throws Exception {
        screen = new DefaultTerminalFactory()
                .createScreen();

        screen.startScreen();
        screen.setCursorPosition(null);

        long lastTick = System.currentTimeMillis();

        while (true) {
            long now = System.currentTimeMillis();
            if (now - lastTick >= 120) {
                updateAnimation();
                draw();
                lastTick = now;
            }

            KeyStroke key = screen.pollInput();
            if (key != null) {
                handleKey(key);
            }

            Thread.sleep(10);
        }
    }

    void draw() throws Exception {
        screen.clear();

        refreshDimensions();
        ensureGrid();
        clampPositions();

        TextGraphics graphics = screen.newTextGraphics();
        drawGrid(graphics);
        drawCommandArea(graphics);

        screen.refresh();
    }

    void drawGrid(final TextGraphics graphics) {
        final int midX = width / 2;
        final int midY = gridHeight / 2;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < width; x++) {
                boolean alive = grid[y][x];
                char cellChar = alive ? '*' : '.';

                if (x == cursorX && y == cursorY) {
                    if (alive) {
                        graphics.setForegroundColor(TextColor.ANSI.GREEN);
                    }
                    graphics.setCharacter(x, y, '[');
                    graphics.setCharacter(x + 1, y, cellChar);
                    graphics.setCharacter(x + 2, y, ']');
                    if (alive) {
                        graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
                    }
                    x += 2;
                } else if (x == midX && y == midY) {
                    graphics.setForegroundColor(alive ? TextColor.ANSI.GREEN : TextColor.ANSI.BLUE);
                    graphics.setCharacter(x, y, cellChar);
                    graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
                } else {
                    if (alive) {
                        graphics.setForegroundColor(TextColor.ANSI.GREEN);
                    }
                    graphics.setCharacter(x, y, cellChar);
                    if (alive) {
                        graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
                    }
                }
            }
        }
    }

    void updateAnimation() {
        if (grid == null || nextGrid == null) return;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < width; x++) {
                int neighbors = countLiveNeighbors(x, y);
                boolean alive = grid[y][x];

                if (alive && neighbors < 2) {
                    nextGrid[y][x] = false;
                } else if (alive && neighbors > 3) {
                    nextGrid[y][x] = false;
                } else if (alive) {
                    nextGrid[y][x] = true;
                } else {
                    nextGrid[y][x] = neighbors == 3;
                }
            }
        }

        boolean[][] tmp = grid;
        grid = nextGrid;
        nextGrid = tmp;
    }

    void drawCommandArea(TextGraphics graphics) {
        int baseY = gridHeight;

        for (int y = baseY; y < height; y++) {
            for (int x = 0; x < width; x++) {
                graphics.setCharacter(x, y, ' ');
            }
        }

        String status = "-- " + mode + " --";
        writeString(graphics, 0, baseY, status);

        String posInfo = "pos(" + cursorX + "," + cursorY + ")=" + getCharAt(cursorX, cursorY);
        int posX = Math.max(0, width - posInfo.length());
        writeString(graphics, posX, baseY, posInfo);

        String liveInfo = "live=" + countActiveCells();
        int liveX = Math.max(0, (width / 2) - (liveInfo.length() / 2));
        writeString(graphics, liveX, baseY, liveInfo);

        if (mode == Mode.COMMAND) {
            writeString(graphics, 0, baseY + 1, ":" + commandBuffer.toString());
        }
    }

    char getCharAt(int x, int y) {
        int midX = width / 2;
        int midY = gridHeight / 2;

        if (x == midX && y == midY) {
            return grid != null && grid[y][x] ? '*' : '.';
        }
        if (x == cursorX && y == cursorY) {
            return grid != null && grid[y][x] ? '*' : '.';
        }
        return grid != null && grid[y][x] ? '*' : '.';
    }

    void writeString(TextGraphics graphics, int x, int y, String s) {
        for (int i = 0; i < s.length() && x + i < width; i++) {
            graphics.setCharacter(x + i, y, s.charAt(i));
        }
    }

    void handleKey(KeyStroke key) {
        if (key == null) return;

        if (mode == Mode.NORMAL) {
            handleNormalMode(key);
        } else {
            handleCommandMode(key);
        }
    }

    void handleNormalMode(KeyStroke key) {
        if (key.getKeyType() == KeyType.Escape) {
            mode = Mode.COMMAND;
            commandBuffer.setLength(0);
            return;
        }

        if (key.getKeyType() == KeyType.Character && key.getCharacter() == ' ') {
            toggleCell(cursorX, cursorY);
            return;
        }

        switch (key.getKeyType()) {
            case ArrowUp -> cursorY = Math.max(0, cursorY - 1);
            case ArrowDown -> cursorY = Math.min(gridHeight - 1, cursorY + 1);
            case ArrowLeft -> cursorX = Math.max(0, cursorX - 1);
            case ArrowRight -> cursorX = Math.min(Math.max(0, width - 3), cursorX + 1);
        }
    }

    void handleCommandMode(KeyStroke key) {
        switch (key.getKeyType()) {
            case Escape -> mode = Mode.NORMAL;
            case Enter -> executeCommand(commandBuffer.toString());
            case Backspace -> {
                if (commandBuffer.length() > 0) {
                    commandBuffer.deleteCharAt(commandBuffer.length() - 1);
                }
            }
            case Character -> commandBuffer.append(key.getCharacter());
        }
    }

    void refreshDimensions() {
        TerminalSize size = screen.getTerminalSize();
        width = Math.max(1, size.getColumns());
        height = Math.max(1, size.getRows());
        gridHeight = Math.max(1, height - CMD_HEIGHT);
    }

    void clampPositions() {
        cursorX = Math.min(Math.max(0, cursorX), Math.max(0, width - 3));
        cursorY = Math.min(Math.max(0, cursorY), Math.max(0, gridHeight - 1));
    }

    void ensureGrid() {
        if (grid != null && grid.length == gridHeight && grid[0].length == width) {
            return;
        }

        boolean[][] newGrid = new boolean[gridHeight][width];
        if (grid != null) {
            int minH = Math.min(gridHeight, grid.length);
            int minW = Math.min(width, grid[0].length);
            for (int y = 0; y < minH; y++) {
                System.arraycopy(grid[y], 0, newGrid[y], 0, minW);
            }
        } else {
            seedPattern(newGrid);
        }
        grid = newGrid;
        nextGrid = new boolean[gridHeight][width];
    }

    void seedPattern(boolean[][] target) {
        double aliveChance = 0.22;
        for (int y = 0; y < target.length; y++) {
            for (int x = 0; x < target[0].length; x++) {
                target[y][x] = random.nextDouble() < aliveChance;
            }
        }
    }

    int countLiveNeighbors(int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= gridHeight) continue;
                if (grid[ny][nx]) count++;
            }
        }
        return count;
    }

    void toggleCell(int x, int y) {
        if (grid == null) return;
        if (x < 0 || y < 0 || x >= width || y >= gridHeight) return;
        grid[y][x] = !grid[y][x];
    }

    int countActiveCells() {
        if (grid == null) return 0;
        int count = 0;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x]) count++;
            }
        }
        return count;
    }

    void executeCommand(String cmd) {
        if (cmd.equals("q") || cmd.equals("quit")) {
            try {
                screen.stopScreen();
                System.exit(0);
            } catch (Exception ignored) {}
        }
        commandBuffer.setLength(0);
        mode = Mode.NORMAL;
    }
}