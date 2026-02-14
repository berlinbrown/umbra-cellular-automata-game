package org.berlin.termsim.plantbasic.models;

public class WaterDroplet {
    private int x;
    private int y;
    private float elementLevel;

    public WaterDroplet(int x, int y, float elementLevel) {
        this.x = x;
        this.y = y;
        this.elementLevel = elementLevel;
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

    public float getElementLevel() {
        return elementLevel;
    }

    public void setElementLevel(float elementLevel) {
        this.elementLevel = elementLevel;
    }
}
