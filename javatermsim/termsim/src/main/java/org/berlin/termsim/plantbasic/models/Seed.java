package org.berlin.termsim.plantbasic.models;

public class Seed {
    private int x;
    private int y;
    private float seedLevel;
    private int groundLiveState = 0;

    public Seed(int x, int y, float seedLevel) {
        this.x = x;
        this.y = y;
        this.seedLevel = seedLevel;
    }

    public Seed(int x, int y, float seedLevel, int groundLiveState) {
        this(x, y, seedLevel);
        this.groundLiveState = groundLiveState;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public float getSeedLevel() {
        return seedLevel;
    }

    public void setSeedLevel(float seedLevel) {
        this.seedLevel = seedLevel;
    }

    public int getGroundLiveState() {
        return groundLiveState;
    }

    public void setGroundLiveState(int groundLiveState) {
        this.groundLiveState = groundLiveState;
    }
}
