package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.ai.AIController;
import com.tankzors.client.gameplay.ai.DifficultyLevel;
import com.tankzors.client.gameplay.interfaces.IBullet;

import java.util.Objects;

/** Một tank = một controller. Parity server entity.AITank. */
public class AITank extends Tank {
    private final DifficultyLevel difficulty;
    private final AIController controller;

    public AITank(String id, double x, double y, DifficultyLevel difficulty) {
        this(id, x, y, difficulty, new AIController());
    }

    public AITank(String id, double x, double y, DifficultyLevel difficulty, AIController controller) {
        super(id, x, y, Objects.requireNonNull(difficulty).getSpeed(), 100, 100, 40, 40);
        this.difficulty = difficulty;
        this.controller = controller;
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public AIController getController() { return controller; }

    public void update(GameWorld state, double deltaTime) {
        controller.update(this, state, deltaTime);
    }

    @Override
    public IBullet shoot() {
        return new Bullet("b-0", id, new Position(position.getX(), position.getY()), rotation,
                Bullet.DEFAULT_SPEED, Bullet.DEFAULT_DAMAGE, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
    }
}
