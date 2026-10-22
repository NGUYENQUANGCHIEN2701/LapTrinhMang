package com.tankzors.client.gameplay.entity;

/** Builds radar/reactor, occupying two by two cells. Owned exclusively by the client world. */
public final class Building {
    public final String id, type;
    public final int col, row, team, maxHp;
    private int hp;
    public Building(String id, String type, int col, int row, int team, int hp) {
        this.id = id; this.type = type; this.col = col; this.row = row; this.team = team;
        this.hp = this.maxHp = Math.max(1, hp);
    }
    public int getHp() { return hp; }
    public boolean isAlive() { return hp > 0; }
    public void takeDamage(int amount) { hp = Math.max(0, hp - Math.max(0, amount)); }
    public boolean contains(int x, int y) { return isAlive() && x >= col && x < col + 2 && y >= row && y < row + 2; }
    public Rectangle bounds(int tileSize) { return new Rectangle(col * tileSize, row * tileSize, tileSize * 2, tileSize * 2); }
}
