package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IMap;

import java.util.ArrayDeque;

/** Route cache per-behavior, dùng distance budget. Parity server ai.AINavigator. */
final class AINavigator {
    enum Result { MOVING, REACHED, UNREACHABLE }

    private final GridPathfinder pathfinder = new GridPathfinder();
    private final ArrayDeque<Position> route = new ArrayDeque<>();
    private GridPathfinder.Cell goal;
    private IMap map;
    private boolean reachable;
    private double retryRemaining;

    void reset() {
        route.clear();
        goal = null;
        map = null;
        retryRemaining = 0;
    }

    Result move(AITank tank, IMap currentMap, Position target, double distance, double dt) {
        GridPathfinder.Cell nextGoal = GridPathfinder.cellAt(target, currentMap);
        retryRemaining = Math.max(0, retryRemaining - dt);
        if (map != currentMap || !nextGoal.equals(goal) || (!reachable && retryRemaining <= 0)) {
            route.clear();
            route.addAll(pathfinder.findPath(tank, currentMap, target));
            reachable = !route.isEmpty();
            retryRemaining = 0.5;
            goal = nextGoal;
            map = currentMap;
        }
        if (!reachable) return Result.UNREACHABLE;
        while (!route.isEmpty()) {
            Position waypoint = route.peek();
            double dx = waypoint.getX() - tank.getPosition().getX();
            double dy = waypoint.getY() - tank.getPosition().getY();
            if (Math.abs(dx) + Math.abs(dy) < 1e-6) {
                route.remove();
                continue;
            }
            if (distance <= 1e-6) return Result.MOVING;
            boolean horizontal = Math.abs(dx) > 1e-6;
            double remaining = horizontal ? dx : dy;
            double step = Math.copySign(Math.min(5.0, Math.min(distance, Math.abs(remaining))), remaining);
            double travelRotation = horizontal ? (step > 0 ? 90 : 270) : (step > 0 ? 180 : 0);
            if (normalizedRotation(tank.getRotation()) != travelRotation) {
                // AI phải dừng nguyên vị trí trong nhịp đổi hướng; nhịp sau
                // mới được tiến thẳng theo hướng vừa quay.
                tank.rotate(travelRotation);
                return Result.MOVING;
            }
            Position from = tank.getPosition();
            Position to = new Position(from.getX() + (horizontal ? step : 0), from.getY() + (horizontal ? 0 : step));
            if (!GridPathfinder.canTraverse(tank, currentMap, from, to)) {
                reachable = false;
                retryRemaining = 0.5;
                route.clear();
                return Result.UNREACHABLE;
            }
            Position valid = Collision.resolveCollision(tank, to, currentMap);
            tank.getPosition().setX(valid.getX());
            tank.getPosition().setY(valid.getY());
            distance -= Math.abs(step);
        }
        return Result.REACHED;
    }

    private static double normalizedRotation(double angle) {
        return ((angle % 360) + 360) % 360;
    }
}
