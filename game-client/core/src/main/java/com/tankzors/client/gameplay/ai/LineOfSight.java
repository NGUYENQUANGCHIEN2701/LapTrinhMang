package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Rectangle;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IMap;

/**
 * Test segment/AABB (slab). Parity server ai.LineOfSight. Pure-float, GWT-safe.
 * Chạm cạnh/góc cũng chặn tầm nhìn.
 */
public final class LineOfSight {
    private LineOfSight() {}

    public static boolean hasClearShot(IMap map, Position from, Position to) {
        if (map == null || from == null || to == null) return false;
        for (Wall wall : map.getWalls()) {
            if (intersects(from, to, wall.getBoundingBox())) return false;
        }
        return true;
    }

    private static boolean intersects(Position from, Position to, Rectangle box) {
        double[] origins = {from.getX(), from.getY()};
        double[] directions = {to.getX() - from.getX(), to.getY() - from.getY()};
        double[] minimums = {box.getX(), box.getY()};
        double[] maximums = {box.getX() + box.getWidth(), box.getY() + box.getHeight()};
        double enter = 0, leave = 1;
        for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(directions[axis]) < 1e-9) {
                if (origins[axis] < minimums[axis] || origins[axis] > maximums[axis]) return false;
            } else {
                double a = (minimums[axis] - origins[axis]) / directions[axis];
                double b = (maximums[axis] - origins[axis]) / directions[axis];
                enter = Math.max(enter, Math.min(a, b));
                leave = Math.min(leave, Math.max(a, b));
                if (enter > leave) return false;
            }
        }
        return true;
    }
}
