package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;
import com.tankzors.client.gameplay.interfaces.IMap;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/**
 * Tìm kiếm cục bộ trong 3 ô quanh last sighting, heading cố định.
 * Parity server ai.PatrolBehavior, nhưng thay java.util.random.RandomGenerator
 * (GWT không emulate) bằng java.util.Random (emulated, seedable).
 */
public final class PatrolBehavior implements IAIBehavior {
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private final Random random;
    private final AINavigator navigator = new AINavigator();
    private Position anchor;
    private Position target;
    private int heading = -1;

    public PatrolBehavior() { this(new Random()); }
    public PatrolBehavior(Random random) { this.random = Objects.requireNonNull(random); }

    public void reset() {
        anchor = null;
        target = null;
        heading = -1;
        navigator.reset();
    }

    public void start(Position lastSeen) {
        reset();
        anchor = new Position(lastSeen.getX(), lastSeen.getY());
    }

    @Override
    public void update(AITank self, GameWorld state, double deltaTime) {
        if (anchor == null || !self.isAlive() || !state.isPlaying()
                || state.getCurrentMap() == null || !Double.isFinite(deltaTime) || deltaTime <= 0) return;
        IMap map = state.getCurrentMap();
        if (target == null) {
            GridPathfinder.Cell current = GridPathfinder.cellAt(self.getPosition(), map);
            GridPathfinder.Cell origin = GridPathfinder.cellAt(anchor, map);
            ArrayList<Integer> choices = new ArrayList<Integer>();
            for (int i = 0; i < DIRECTIONS.length; i++) {
                GridPathfinder.Cell next = new GridPathfinder.Cell(current.col + DIRECTIONS[i][0],
                        current.row + DIRECTIONS[i][1]);
                if (Math.abs(next.col - origin.col) <= 3 && Math.abs(next.row - origin.row) <= 3
                        && GridPathfinder.canOccupy(self, map, next)
                        && GridPathfinder.canTraverse(self, map, current.center(map), next.center(map))) {
                    choices.add(i);
                }
            }
            if (choices.isEmpty()) return;
            if (!choices.contains(heading)) {
                if (choices.size() > 1 && heading >= 0) choices.remove(Integer.valueOf((heading + 2) % 4));
                heading = choices.get(random.nextInt(choices.size()));
            }
            target = new GridPathfinder.Cell(current.col + DIRECTIONS[heading][0],
                    current.row + DIRECTIONS[heading][1]).center(map);
        }
        AINavigator.Result result = navigator.move(self, map, target, self.getSpeed() * deltaTime, deltaTime);
        if (result != AINavigator.Result.MOVING) {
            target = null;
            navigator.reset();
            if (result == AINavigator.Result.UNREACHABLE) heading = -1;
        }
    }

    @Override public String getBehaviorName() { return "PATROL"; }
}
