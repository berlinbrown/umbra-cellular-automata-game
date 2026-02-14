/**
 * @Copyright 2026 - Berlin Brown
 */
package org.berlin.termsim.plantbasic;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.berlin.termsim.plantbasic.models.Seed;
import org.berlin.termsim.plantbasic.models.WaterDroplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Terminal Simulator cellular automata plant.
 * Current state includes water and seed droplets hitting the ground and increasing
 * the water level in the soil.  The seeds in next phase will sprout into plants.
 */
public class TermSimCellularAutomataPlantApp {

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
    private final StringBuilder commandBuffer = new StringBuilder();

    private final List<WaterDroplet> waterDroplets = new ArrayList<>();
    private final List<WaterDroplet> groundWaterDroplets = new ArrayList<>();
    private final List<Seed> seeds = new ArrayList<>();
    private final List<Seed> groundSeeds = new ArrayList<>();
    private final Random random = new Random();

    private int spawnCounter = 0;
    private int spawnInterval = 20;
    private int seedSpawnCounter = 0;
    private int seedSpawnInterval = 25;
    private float totalWaterLevel = 0.0f;
    private float totalSeedLevel = 0.0f;

    private Screen screen;

    public static void main(final String[] args) throws Exception {
        new TermSimCellularAutomataPlantApp().start();
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

        this.refreshDimensions();
        this.clampPositions();

        final TextGraphics graphics = screen.newTextGraphics();
        this.drawGrid(graphics);
        this.drawWaterDroplets(graphics);
        this.drawSeeds(graphics);
        this.drawCommandArea(graphics);

        screen.refresh();
    }

    void drawGrid(final TextGraphics graphics) {
        int midX = width / 2;
        int midY = gridHeight / 2;
        int groundStartY = Math.max(0, gridHeight - 4);

        for (int y = 0; y < gridHeight; y++) {
            final boolean isGroundRow = y >= groundStartY;
            if (isGroundRow) {
                graphics.setForegroundColor(TextColor.ANSI.MAGENTA);
            } else {
                graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }
            for (int x = 0; x < width; x++) {
                if (x == midX && y == midY) {
                    graphics.setForegroundColor(TextColor.ANSI.BLUE);
                    graphics.setCharacter(x, y, '@');
                    graphics.setForegroundColor(isGroundRow ? TextColor.ANSI.MAGENTA : TextColor.ANSI.DEFAULT);
                } else if (x == cursorX && y == cursorY) {
                    graphics.setCharacter(x, y, '[');
                    graphics.setCharacter(x + 1, y, isGroundRow ? '%' : '.');
                    graphics.setCharacter(x + 2, y, ']');
                    x += 2;
                } else {
                    graphics.setCharacter(x, y, isGroundRow ? '%' : '.');
                }
            }
        }
    }

    void drawWaterDroplets(final TextGraphics graphics) {
        for (final WaterDroplet droplet : waterDroplets) {
            if (droplet.getY() >= 0 && droplet.getY() < gridHeight && droplet.getX() >= 0
                    && droplet.getX() < width) {
                graphics.setForegroundColor(TextColor.ANSI.BLUE);
                graphics.setCharacter(droplet.getX(), droplet.getY(), '#');
                graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }
        }

        for (final WaterDroplet droplet : groundWaterDroplets) {
            if (droplet.getY() >= 0 && droplet.getY() < gridHeight && droplet.getX() >= 0
                    && droplet.getX() < width) {
                graphics.setForegroundColor(TextColor.ANSI.BLUE);
                graphics.setCharacter(droplet.getX(), droplet.getY(), '#');
                graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }
        }
    }

    void drawSeeds(final TextGraphics graphics) {
        for (final Seed seed : seeds) {
            if (seed.getY() >= 0 && seed.getY() < gridHeight && seed.getX() >= 0
                    && seed.getX() < width) {
                graphics.setForegroundColor(TextColor.ANSI.GREEN);
                graphics.setCharacter(seed.getX(), seed.getY(), 's');
                graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }
        }

        for (final Seed seed : groundSeeds) {
            if (seed.getY() >= 0 && seed.getY() < gridHeight && seed.getX() >= 0
                    && seed.getX() < width) {
                graphics.setForegroundColor(TextColor.ANSI.GREEN);
                graphics.setCharacter(seed.getX(), seed.getY(), 's');
                graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }
        }
    }

    void updateAnimation() {
        int groundStartY = Math.max(0, gridHeight - 4);
        // Update existing water droplets
        for (final WaterDroplet droplet : new ArrayList<>(waterDroplets)) {
            droplet.setY(droplet.getY() + 1);
            // Remove droplet if it reaches ground and accumulate water
            if (droplet.getY() >= groundStartY) {
                totalWaterLevel += droplet.getElementLevel();
                droplet.setY(groundStartY);
                groundWaterDroplets.add(droplet);
                waterDroplets.remove(droplet);
            }
        }

        for (final WaterDroplet droplet : new ArrayList<>(groundWaterDroplets)) {
            droplet.setGroundLiveState(droplet.getGroundLiveState() - 1);
            if (droplet.getGroundLiveState() <= 0) {
                groundWaterDroplets.remove(droplet);
            }
        }

        // Update seeds
        for (final Seed seed : new ArrayList<>(seeds)) {
            seed.setY(seed.getY() + 1);
            if (seed.getY() >= groundStartY) {
                totalSeedLevel += seed.getSeedLevel();
                seed.setY(groundStartY);
                groundSeeds.add(seed);
                seeds.remove(seed);
            }
        }

        for (final Seed seed : new ArrayList<>(groundSeeds)) {
            seed.setGroundLiveState(seed.getGroundLiveState() - 1);
            if (seed.getGroundLiveState() <= 0) {
                groundSeeds.remove(seed);
            }
        }

        // Spawn new droplets at random intervals
        spawnCounter++;
        if (spawnCounter >= spawnInterval) {

            // Reset
            final int randomX = random.nextInt(width);
            final float randomLevel = random.nextFloat() * 40.0f;
            final int groundLiveState = random.nextInt(13);
            waterDroplets.add(new WaterDroplet(randomX, 0, randomLevel, groundLiveState));
            spawnCounter = 0;
            // Vary spawn interval for more natural effect
            spawnInterval = 5 + random.nextInt(14);
        }

        seedSpawnCounter++;
        if (seedSpawnCounter >= seedSpawnInterval) {
            final int randomX = random.nextInt(width);
            final float randomLevel = random.nextFloat() * 40.0f;
            final int groundLiveState = random.nextInt(13);
            seeds.add(new Seed(randomX, 0, randomLevel, groundLiveState));
            seedSpawnCounter = 0;
            seedSpawnInterval = 7 + random.nextInt(18);
        }
    }

    void drawCommandArea(final TextGraphics graphics) {
        final int baseY = gridHeight;
        for (int y = baseY; y < height; y++) {
            for (int x = 0; x < width; x++) {
                graphics.setCharacter(x, y, ' ');
            }
        }
        final String status = "-- " + mode + " --";
        this.writeString(graphics, 0, baseY, status);

        final String posInfo = "pos(" + cursorX + "," + cursorY + ")=Char:(" + getCharAt(cursorX, cursorY) + ")";
        int posX = Math.max(0, width - posInfo.length());
        this.writeString(graphics, posX, baseY, posInfo);

        if (mode == Mode.COMMAND) {
            this.writeString(graphics, 0, baseY + 1, ":" + commandBuffer.toString());
        }

        final String waterInfo = "Water Level: " + String.format("%.2f", totalWaterLevel);
        final int waterX = Math.max(0, width - waterInfo.length());
        writeString(graphics, waterX, baseY + 1, waterInfo);

        final String seedInfo = "Seed Level: " + String.format("%.2f", totalSeedLevel);
        final int seedX = Math.max(0, width - seedInfo.length());
        writeString(graphics, seedX, baseY + 2, seedInfo);

        final int groundStartY = Math.max(0, gridHeight - 4);
        // Check if any water droplets reached ground
        for (final WaterDroplet droplet : groundWaterDroplets) {
            if (droplet.getY() >= groundStartY && droplet.getY() < gridHeight) {
                String message = "Ground Reached at Level";
                int messageX = Math.max(0, (width - message.length()) / 2);
                int messageY = Math.min(height - 1, baseY + 3);
                this.writeString(graphics, messageX, messageY, message);
                break;
            }
        }
    }

    char getCharAt(final int x, final int y) {
        int midX = width / 2;
        int midY = gridHeight / 2;
        int groundStartY = Math.max(0, gridHeight - 4);

        // Check for water droplets
        for (final WaterDroplet droplet : waterDroplets) {
            if (x == droplet.getX() && y == droplet.getY()) {
                return '#';
            }
        }
        for (final WaterDroplet droplet : groundWaterDroplets) {
            if (x == droplet.getX() && y == droplet.getY()) {
                return '#';
            }
        }
        for (final Seed seed : seeds) {
            if (x == seed.getX() && y == seed.getY()) {
                return 's';
            }
        }
        for (final Seed seed : groundSeeds) {
            if (x == seed.getX() && y == seed.getY()) {
                return 's';
            }
        }
        if (x == midX && y == midY) {
            return '@';
        }
        if (y >= groundStartY) {
            return '%';
        }
        return '.';
    }

    void writeString(TextGraphics graphics, int x, int y, String s) {
        for (int i = 0; i < s.length() && x + i < width; i++) {
            graphics.setCharacter(x + i, y, s.charAt(i));
        }
    }

    void handleKey(final KeyStroke key) {
        if (key == null) {
            return;
        }
        if (mode == Mode.NORMAL) {
            handleNormalMode(key);
        } else {
            handleCommandMode(key);
        }
    }

    void handleNormalMode(final KeyStroke key) {
        if (key.getKeyType() == KeyType.Escape) {
            mode = Mode.COMMAND;
            commandBuffer.setLength(0);
            return;
        }
        switch (key.getKeyType()) {
            case ArrowUp -> cursorY = Math.max(0, cursorY - 1);
            case ArrowDown -> cursorY = Math.min(gridHeight - 1, cursorY + 1);
            case ArrowLeft -> cursorX = Math.max(0, cursorX - 1);
            case ArrowRight -> cursorX = Math.min(Math.max(0, width - 3), cursorX + 1);
            default -> {
            }
        }
    }

    void handleCommandMode(final KeyStroke key) {
        switch (key.getKeyType()) {
            case Escape -> mode = Mode.NORMAL;
            case Enter -> executeCommand(commandBuffer.toString());
            case Backspace -> {
                if (!commandBuffer.isEmpty()) {
                    commandBuffer.deleteCharAt(commandBuffer.length() - 1);
                }
            }
            case Character -> commandBuffer.append(key.getCharacter());
            default -> {
            }
        }
    }

    void refreshDimensions() {
        final TerminalSize size = screen.getTerminalSize();
        width = Math.max(1, size.getColumns());
        height = Math.max(1, size.getRows());
        gridHeight = Math.max(1, height - CMD_HEIGHT);
    }

    void clampPositions() {
        cursorX = Math.min(Math.max(0, cursorX), Math.max(0, width - 3));
        cursorY = Math.min(Math.max(0, cursorY), Math.max(0, gridHeight - 1));
    }

    void executeCommand(final String cmd) {
        if (cmd.equals("q") || cmd.equals("quit")) {
            try {
                screen.stopScreen();
                System.exit(0);
            } catch (Exception ignored) {
            }
        }
        commandBuffer.setLength(0);
        mode = Mode.NORMAL;
    }
}