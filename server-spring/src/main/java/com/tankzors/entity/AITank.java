package com.tankzors.entity;

import com.tankzors.ai.AIController;
import com.tankzors.ai.DifficultyLevel;
import com.tankzors.engine.GameState;
import com.tankzors.interfaces.IBullet;
import java.util.Objects;

/** One tank owns one controller: reaction timers and paths are never shared. */
public class AITank extends Tank {
    private final DifficultyLevel difficulty;
    private final AIController controller = new AIController();

    public AITank(String id, double x, double y, DifficultyLevel difficulty) {
        super(id, x, y, Objects.requireNonNull(difficulty).getSpeed(), 100, 100, 40, 40);
        this.difficulty = difficulty;
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public AIController getController() { return controller; }

    public void update(GameState state, double deltaTime) {
        controller.update(this, state, deltaTime);
    }

    @Override
    public IBullet shoot() {
        return new com.tankzors.entity.Bullet(id, new Position(position.getX(), position.getY()), rotation);
    }
}
