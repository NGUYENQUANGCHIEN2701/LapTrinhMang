package com.tankzors.ai;

import com.tankzors.engine.CollisionManager;
import com.tankzors.entity.AITank;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IMap;
import java.util.*;

/** Four-neighbor BFS; both nodes and edges must fit the tank footprint. */
public final class GridPathfinder {
    public record Cell(int col, int row) {
        public Position center(IMap map) {
            return new Position((col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize());
        }
    }
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public static Cell cellAt(Position position, IMap map) {
        return new Cell((int) Math.floor(position.getX() / map.getTileSize()),
                (int) Math.floor(position.getY() / map.getTileSize()));
    }

    public List<Position> findPath(AITank tank, IMap map, Position target) {
        if (map == null || target == null) return List.of();
        Cell start = cellAt(tank.getPosition(), map);
        Cell goal = cellAt(target, map);
        if (!canOccupy(tank, map, start) || !canOccupy(tank, map, goal)) return List.of();
        Map<Cell, Cell> previous = new HashMap<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        previous.put(start, start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            if (current.equals(goal)) {
                LinkedList<Position> path = new LinkedList<>();
                for (Cell cell = goal; !cell.equals(start); cell = previous.get(cell)) {
                    path.addFirst(cell.center(map));
                }
                // Recenter before turning; never cut diagonally across an inside corner.
                path.addFirst(start.center(map));
                // A moving target may change cells mid-step. Keep going forward when
                // the next center is directly reachable, instead of walking backward
                // to our own cell center on every replan.
                if (path.size() > 1 && canTraverse(tank, map, tank.getPosition(), path.get(1))) {
                    path.removeFirst();
                }
                return path;
            }
            for (int[] direction : DIRECTIONS) {
                Cell next = new Cell(current.col() + direction[0], current.row() + direction[1]);
                if (!previous.containsKey(next) && canOccupy(tank, map, next)
                        && canTraverse(tank, map, current.center(map), next.center(map))) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    public static boolean canOccupy(AITank tank, IMap map, Cell cell) {
        return map.isWalkable(cell.col(), cell.row())
                && !CollisionManager.checkTankWallCollision(tank, map, cell.center(map));
    }

    public static boolean canTraverse(AITank tank, IMap map, Position from, Position to) {
        // Edges are axis-aligned. A swept tank is the rectangle covering both endpoints.
        if (Math.abs(from.getX() - to.getX()) > 1e-6
                && Math.abs(from.getY() - to.getY()) > 1e-6) return false;
        var startBox = CollisionManager.getTankBoundingBoxAt(tank, from);
        var endBox = CollisionManager.getTankBoundingBoxAt(tank, to);
        double left = Math.min(startBox.getX(), endBox.getX());
        double top = Math.min(startBox.getY(), endBox.getY());
        double right = Math.max(startBox.getX(), endBox.getX()) + startBox.getWidth();
        double bottom = Math.max(startBox.getY(), endBox.getY()) + startBox.getHeight();
        if (left < 0 || top < 0 || right > map.getWidth() * map.getTileSize()
                || bottom > map.getHeight() * map.getTileSize()) return false;
        var swept = new com.tankzors.entity.Rectangle(left, top, right - left, bottom - top);
        for (var wall : map.getWalls()) {
            if (swept.intersects(wall.getBoundingBox())) return false;
        }
        int size = map.getTileSize();
        for (int row = (int) Math.floor(top / size); row < (int) Math.ceil(bottom / size); row++) {
            for (int col = (int) Math.floor(left / size); col < (int) Math.ceil(right / size); col++) {
                if (!map.isWalkable(col, row)) return false;
            }
        }
        return true;
    }
}
