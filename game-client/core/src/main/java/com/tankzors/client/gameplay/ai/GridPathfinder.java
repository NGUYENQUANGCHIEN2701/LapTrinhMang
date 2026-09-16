package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Rectangle;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BFS 4-hàng xóm. Parity server ai.GridPathfinder, nhưng đổi Cell từ record
 * (GWT 2.10 không dịch) sang class thường với equals/hashCode thủ công.
 */
public final class GridPathfinder {
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /** Ô lưới (col, row). Class thường thay record cho GWT. */
    public static final class Cell {
        public final int col;
        public final int row;

        public Cell(int col, int row) { this.col = col; this.row = row; }

        public Position center(IMap map) {
            return new Position((col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize());
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cell)) return false;
            Cell cell = (Cell) o;
            return col == cell.col && row == cell.row;
        }

        @Override public int hashCode() { return Objects.hash(col, row); }
    }

    public static Cell cellAt(Position position, IMap map) {
        return new Cell((int) Math.floor(position.getX() / map.getTileSize()),
                (int) Math.floor(position.getY() / map.getTileSize()));
    }

    public List<Position> findPath(AITank tank, IMap map, Position target) {
        if (map == null || target == null) return new ArrayList<Position>();
        Cell start = cellAt(tank.getPosition(), map);
        Cell goal = cellAt(target, map);
        if (!canOccupy(tank, map, start) || !canOccupy(tank, map, goal)) return new ArrayList<Position>();

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
                // Recenter trước khi rẽ; không cắt chéo góc trong.
                path.addFirst(start.center(map));
                if (path.size() > 1 && canTraverse(tank, map, tank.getPosition(), path.get(1))) {
                    path.removeFirst();
                }
                return path;
            }
            for (int[] direction : DIRECTIONS) {
                Cell next = new Cell(current.col + direction[0], current.row + direction[1]);
                if (!previous.containsKey(next) && canOccupy(tank, map, next)
                        && canTraverse(tank, map, current.center(map), next.center(map))) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        return new ArrayList<Position>();
    }

    public static boolean canOccupy(AITank tank, IMap map, Cell cell) {
        return map.isWalkable(cell.col, cell.row)
                && !Collision.checkTankWallCollision(tank, map, cell.center(map));
    }

    public static boolean canTraverse(AITank tank, IMap map, Position from, Position to) {
        // Edge phải axis-aligned.
        if (Math.abs(from.getX() - to.getX()) > 1e-6 && Math.abs(from.getY() - to.getY()) > 1e-6) return false;
        Rectangle startBox = Collision.getTankBoundingBoxAt(tank, from);
        Rectangle endBox = Collision.getTankBoundingBoxAt(tank, to);
        double left = Math.min(startBox.getX(), endBox.getX());
        double top = Math.min(startBox.getY(), endBox.getY());
        double right = Math.max(startBox.getX(), endBox.getX()) + startBox.getWidth();
        double bottom = Math.max(startBox.getY(), endBox.getY()) + startBox.getHeight();
        if (left < 0 || top < 0 || right > map.getWidth() * map.getTileSize()
                || bottom > map.getHeight() * map.getTileSize()) return false;
        Rectangle swept = new Rectangle(left, top, right - left, bottom - top);
        for (Wall wall : map.getWalls()) {
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
