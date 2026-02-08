package org.berlin.termsim;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

public class TermSimApp {

    static final int WIDTH = 80;
    static final int HEIGHT = 25;
    static final int GRID_HEIGHT = 18;
    static final int CMD_HEIGHT = 7;

    enum Mode {
        NORMAL, COMMAND
    }

    private Mode mode = Mode.NORMAL;
    private int cursorX = 0;
    private int cursorY = 0;
    private StringBuilder commandBuffer = new StringBuilder();

    private int movingX = WIDTH - 2;
    private int movingY = 0;

    private Screen screen;

    public static void main(String[] args) throws Exception {
        new TermSimApp().start();
    }

    void start() throws Exception {
        screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(WIDTH, HEIGHT))
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

        TextGraphics graphics = screen.newTextGraphics();
        drawGrid(graphics);
        drawMovingChar(graphics);
        drawCommandArea(graphics);

        screen.refresh();
    }

    void drawGrid(TextGraphics graphics) {
        int midX = WIDTH / 2;
        int midY = GRID_HEIGHT / 2;

        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (x == midX && y == midY) {
                    graphics.setForegroundColor(TextColor.ANSI.BLUE);
                    graphics.setCharacter(x, y, '@');
                    graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
                } else if (x == cursorX && y == cursorY) {
                    graphics.setCharacter(x, y, '[');
                    graphics.setCharacter(x + 1, y, '.');
                    graphics.setCharacter(x + 2, y, ']');
                    x += 2;
                } else {
                    graphics.setCharacter(x, y, '.');
                }
            }
        }
    }

    void drawMovingChar(TextGraphics graphics) {
        if (movingY >= 0 && movingY < GRID_HEIGHT && movingX >= 0 && movingX < WIDTH) {
            graphics.setForegroundColor(TextColor.ANSI.BLUE);
            graphics.setCharacter(movingX, movingY, '#');
            graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
        }
    }

    void updateAnimation() {
        movingY += 1;
        if (movingY > GRID_HEIGHT - 1) {
            movingY = 0;
        }
    }

    void drawCommandArea(TextGraphics graphics) {
        int baseY = GRID_HEIGHT;

        for (int y = baseY; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                graphics.setCharacter(x, y, ' ');
            }
        }

        String status = "-- " + mode + " --";
        writeString(graphics, 0, baseY, status);

        if (mode == Mode.COMMAND) {
            writeString(graphics, 0, baseY + 1, ":" + commandBuffer.toString());
        }
    }

    void writeString(TextGraphics graphics, int x, int y, String s) {
        for (int i = 0; i < s.length() && x + i < WIDTH; i++) {
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

        switch (key.getKeyType()) {
            case ArrowUp -> cursorY = Math.max(0, cursorY - 1);
            case ArrowDown -> cursorY = Math.min(GRID_HEIGHT - 1, cursorY + 1);
            case ArrowLeft -> cursorX = Math.max(0, cursorX - 1);
            case ArrowRight -> cursorX = Math.min(WIDTH - 3, cursorX + 1);
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