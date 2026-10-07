package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WeaponProfile;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Rectangle;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.TileType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Port AI xe tăng của Builds (game.Bot + game.Tank trong kha.js), chạy theo
 * frame 60 Hz như bản gốc: mỗi fixed step 1/30 s của simulation chạy hai frame.
 *
 * <ul>
 *   <li>Đi theo từng ô; mỗi ô mất floor(100 / (speed × speedRatio nền)) frame.</li>
 *   <li>Quay từng nấc 22.5°, mỗi nấc chờ rotationDelay frame (+1 trên cát).</li>
 *   <li>Phát hiện địch theo 4 tia thẳng: 10 ô phía trước, 6 ô các hướng khác,
 *       bị vật cản che; thấy thì quay về hướng đó, phát đầu sau 20 frame, rồi mỗi 60 frame.</li>
 *   <li>Không thấy ai: đi thẳng, bị chặn thì rẽ trái/phải ngẫu nhiên, hết đường thì quay đầu.</li>
 *   <li>Địch trong vùng thoi 5 ô (bỏ qua tường): tìm đường Dijkstra có giá xuyên gạch,
 *       gặp gạch trên đường thì bắn phá; tính lại đường sau 120 frame.</li>
 *   <li>Bị bắn khi không giao chiến: quay về phía phát đạn (attackedSide).</li>
 *   <li>Đồng minh FollowPlayer: cứ 120 frame lại tìm đường tới ô người chơi.</li>
 * </ul>
 */
public final class BuildsBotBrain {

    public static final double FRAME_SECONDS = 1.0 / 60.0;
    static final int PATH_COOLDOWN = 120;
    static final int SIGHT_LINE = 10;
    static final int SEARCH_DISTANCE = 5;
    static final int FIRST_SHOT_DELAY = 20;
    private static final int MAX_COST = 99999;
    private static final int[] OFF_X = {1, -1, 0, 0};
    private static final int[] OFF_Y = {0, 0, 1, -1};

    enum Motion { STANDING, MOVING, ROTATING }
    enum AiState { SEARCHING, FORCE_ROTATION, ATTACK }

    /** Nguồn id đạn deterministic do simulation cấp. */
    public interface BulletIdSource {
        String nextId(String ownerId);
    }

    private final Random rng;
    private BulletIdSource bulletIds;

    private boolean initialized;
    private int col;
    private int row;
    private int dirX;
    private int dirY = -1;
    private Motion motion = Motion.STANDING;
    private int nextCol;
    private int nextRow;
    private double angle;
    private int rotationDelay;
    private int maxRotationDelay;
    private double speedRatio = 0.8;
    private int reloadDelay;
    private AiState aiState = AiState.SEARCHING;
    private int forceX;
    private int forceY;
    /** Đường đi: phần tử cuối là ô kế tiếp (giống mảng turns của Builds). */
    private final List<int[]> path = new ArrayList<>();
    private int pathCooldown;
    private double frameAccumulator;

    public BuildsBotBrain(long seed) {
        this.rng = new Random(seed);
    }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIds = source; }

    public void update(AITank self, GameWorld world, double dt) {
        IMap map = world.getCurrentMap();
        if (map == null || !self.isAlive()) return;
        if (!initialized) initialize(self, map);
        frameAccumulator += dt;
        while (frameAccumulator + 1e-9 >= FRAME_SECONDS) {
            frameAccumulator -= FRAME_SECONDS;
            frame(self, world, map);
            if (!self.isAlive()) return;
        }
    }

    /** Quên vị trí/đường đi; lần update sau khởi tạo lại từ vị trí xe. */
    public void reset() {
        initialized = false;
        motion = Motion.STANDING;
        aiState = AiState.SEARCHING;
        path.clear();
        pathCooldown = 0;
        reloadDelay = 0;
        frameAccumulator = 0;
    }

    /** Builds attackedSide: bị trúng đạn khi chưa giao chiến thì quay về phía bắn. */
    public void onAttacked(double bulletDirection) {
        if (aiState == AiState.ATTACK) return;
        int[] d = cardinal(bulletDirection);
        forceX = -d[0];
        forceY = -d[1];
        aiState = AiState.FORCE_ROTATION;
    }

    /** Ô xe đang đứng hoặc đã giữ chỗ để đi tới (teamsGrid của Builds). */
    public boolean occupies(int c, int r) {
        if (!initialized) return false;
        return (c == col && r == row) || (motion == Motion.MOVING && c == nextCol && r == nextRow);
    }

    // ===== Tank layer =====
    private void initialize(AITank self, IMap map) {
        int ts = map.getTileSize();
        col = (int) Math.floor(self.getPosition().getX() / ts);
        row = (int) Math.floor(self.getPosition().getY() / ts);
        self.getPosition().setX((col + 0.5) * ts);
        self.getPosition().setY((row + 0.5) * ts);
        int[] d = cardinal(self.getRotation());
        dirX = d[0];
        dirY = d[1];
        angle = rotationOf(dirX, dirY);
        self.rotate(angle);
        initialized = true;
        setStanding(self, map);
    }

    private void frame(AITank self, GameWorld world, IMap map) {
        if (reloadDelay > 0) reloadDelay--;
        switch (motion) {
            case MOVING:
                moveToNextCell(self, map);
                break;
            case ROTATING:
                rotationDelay--;
                if (rotationDelay > 0) return;
                rotationDelay = maxRotationDelay;
                double target = rotationOf(dirX, dirY);
                double diff = ((target - angle) % 360 + 540) % 360 - 180;
                if (Math.abs(diff) > 1e-6) {
                    angle = ((angle + (diff > 0 ? 22.5 : -22.5)) % 360 + 360) % 360;
                    self.rotate(angle);
                    return;
                }
                angle = target;
                self.rotate(angle);
                setStanding(self, map);
                break;
            default:
                break;
        }
        afterUpdate(self, world, map);
    }

    private void setStanding(AITank self, IMap map) {
        motion = Motion.STANDING;
        int ts = map.getTileSize();
        speedRatio = map.speedFactorAt((col + 0.5) * ts, (row + 0.5) * ts) * 0.8;
        int def = BuildsTankStats.rotationDelay(self.getBuildsType());
        maxRotationDelay = speedRatio <= 0.5 ? def + 1 : def;
        rotationDelay = maxRotationDelay;
    }

    private void moveToNextCell(AITank self, IMap map) {
        int ts = map.getTileSize();
        double speed = self.getBuildsSpeed() * speedRatio;
        int frames = Math.max(1, (int) Math.floor(100.0 / speed));
        double step = (double) ts / frames;
        double tx = (nextCol + 0.5) * ts;
        double ty = (nextRow + 0.5) * ts;
        Position p = self.getPosition();
        double nx = approach(p.getX(), tx, step);
        double ny = approach(p.getY(), ty, step);
        p.setX(nx);
        p.setY(ny);
        if (Math.abs(nx - tx) < 1e-6 && Math.abs(ny - ty) < 1e-6) {
            col = nextCol;
            row = nextRow;
            setStanding(self, map);
        }
    }

    private static double approach(double value, double target, double step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }

    private void move(AITank self, GameWorld world, IMap map, int dx, int dy) {
        setDirection(dx, dy);
        if (motion != Motion.STANDING) return;
        int tx = col + dx;
        int ty = row + dy;
        if (!isFree(self, world, map, tx, ty)) return;
        motion = Motion.MOVING;
        nextCol = tx;
        nextRow = ty;
    }

    private void setDirection(int dx, int dy) {
        if (motion != Motion.STANDING) return;
        if (dirX == dx && dirY == dy) return;
        motion = Motion.ROTATING;
        dirX = dx;
        dirY = dy;
    }

    private void fire(AITank self, GameWorld world) {
        if (motion == Motion.ROTATING || reloadDelay > 0) return;
        double direction = rotationOf(dirX, dirY);
        WeaponProfile weapon = WeaponProfile.fromItemId(
                BuildsTankStats.bulletType(self.getBuildsType(), self.getBuildsLevel()));
        boolean horizontal = direction == 90 || direction == 270;
        String id = bulletIds != null ? bulletIds.nextId(self.getId()) : "b-" + self.getId();
        world.addBullet(new Bullet(id, self.getId(),
                new Position(self.getPosition().getX(), self.getPosition().getY()), direction,
                weapon.speedPixelsPerSecond(), weapon.getDamage(), horizontal ? 8 : 4, horizontal ? 4 : 8,
                weapon.getItemId()));
        reloadDelay = BuildsTankStats.RELOAD_FRAMES;
    }

    // ===== Bot layer =====
    private void afterUpdate(AITank self, GameWorld world, IMap map) {
        if (pathCooldown > 0) {
            pathCooldown--;
        } else if (aiState != AiState.ATTACK && self.isFollowPlayer()) {
            PlayerTank leader = nearestPlayer(self, world);
            if (leader != null) {
                int ts = map.getTileSize();
                setDestination(self, world, map, cellOf(leader.getPosition().getX(), ts),
                        cellOf(leader.getPosition().getY(), ts));
            }
        }
        switch (aiState) {
            case SEARCHING:
                findEnemy(self, world, map);
                if (!path.isEmpty()) updatePath(self, world, map);
                if (aiState == AiState.ATTACK) return;
                move(self, world, map, dirX, dirY);
                if (motion == Motion.STANDING) {
                    int[] turn = findRotation(self, world, map);
                    if (turn != null) setDirection(turn[0], turn[1]);
                }
                break;
            case FORCE_ROTATION:
                if (motion == Motion.STANDING) {
                    setDirection(forceX, forceY);
                    aiState = AiState.SEARCHING;
                }
                break;
            case ATTACK:
                findEnemy(self, world, map);
                if (!path.isEmpty()) updatePath(self, world, map);
                if (aiState == AiState.ATTACK) fire(self, world);
                break;
            default:
                break;
        }
    }

    private void findEnemy(AITank self, GameWorld world, IMap map) {
        if (motion != Motion.STANDING) return;
        checkSightLines(self, world, map);
        if (aiState == AiState.ATTACK) {
            if (!path.isEmpty()) {
                path.clear();
                pathCooldown = PATH_COOLDOWN;
            }
            return;
        }
        if (pathCooldown > 0 || !path.isEmpty()) return;
        // Quét vùng thoi theo từng vòng như Bot.findEnemy, bỏ qua tường.
        for (int i = 1; i <= SEARCH_DISTANCE; i++) {
            for (int ix = col - i; ix <= col + i; ix++) {
                if (tryTarget(self, world, map, ix, row - i)) return;
                if (tryTarget(self, world, map, ix, row + i)) return;
            }
            for (int iy = row - i + 1; iy < row + i; iy++) {
                if (tryTarget(self, world, map, col - i, iy)) return;
                if (tryTarget(self, world, map, col + i, iy)) return;
            }
        }
    }

    private boolean tryTarget(AITank self, GameWorld world, IMap map, int x, int y) {
        if (Math.abs(col - x) + Math.abs(row - y) > SEARCH_DISTANCE) return false;
        if (!hasHostileAt(self, world, map, x, y)) return false;
        setDestination(self, world, map, x, y);
        return true;
    }

    private void checkSightLines(AITank self, GameWorld world, IMap map) {
        if (checkSightLine(self, world, map, 0, -1)) return;
        if (checkSightLine(self, world, map, 0, 1)) return;
        if (checkSightLine(self, world, map, -1, 0)) return;
        if (checkSightLine(self, world, map, 1, 0)) return;
        aiState = AiState.SEARCHING;
    }

    private boolean checkSightLine(AITank self, GameWorld world, IMap map, int sx, int sy) {
        int dist = (dirX == sx && dirY == sy) ? SIGHT_LINE : (SIGHT_LINE + 1) / 2 + 1;
        for (int k = 1; k < dist; k++) {
            int x = col + sx * k;
            int y = row + sy * k;
            if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) break;
            if (isViewWall(map, x, y)) break;
            if (hasHostileAt(self, world, map, x, y)) {
                setDirection(sx, sy);
                if (aiState != AiState.ATTACK) reloadDelay = FIRST_SHOT_DELAY;
                aiState = AiState.ATTACK;
                return true;
            }
        }
        return false;
    }

    private void updatePath(AITank self, GameWorld world, IMap map) {
        if (motion != Motion.STANDING || aiState == AiState.ATTACK) return;
        int[] next = path.get(path.size() - 1);
        int ndx = next[0] - col;
        int ndy = next[1] - row;
        if (Math.abs(ndx + ndy) != 1 || (ndx != 0 && ndy != 0)) {
            path.clear();
            pathCooldown = PATH_COOLDOWN;
            return;
        }
        if (dirX == ndx && dirY == ndy) {
            move(self, world, map, dirX, dirY);
            if (motion == Motion.MOVING) {
                path.remove(path.size() - 1);
            } else if (!isOccupied(self, world, map, next[0], next[1])) {
                // Vật cản (gạch/bụi) chắn đường: chuyển sang tấn công để bắn phá.
                aiState = AiState.ATTACK;
            } else {
                path.clear();
                pathCooldown = PATH_COOLDOWN;
            }
        } else {
            setDirection(ndx, ndy);
        }
    }

    private int[] findRotation(AITank self, GameWorld world, IMap map) {
        int[] left = {dirX == 0 ? -1 : 0, dirY == 0 ? -1 : 0};
        int[] right = {dirX == 0 ? 1 : 0, dirY == 0 ? 1 : 0};
        if (rng.nextInt(2) == 0) {
            int[] temp = left;
            left = right;
            right = temp;
        }
        if (isFree(self, world, map, col + left[0], row + left[1])) return left;
        if (isFree(self, world, map, col + right[0], row + right[1])) return right;
        int[] down = {-dirX, -dirY};
        if (isFree(self, world, map, col + down[0], row + down[1])) return down;
        return null;
    }

    // ===== Dijkstra (game.Dijkstra.createPath + Bot.genDepthMap) =====
    private void setDestination(AITank self, GameWorld world, IMap map, int endX, int endY) {
        int startX = motion == Motion.MOVING ? nextCol : col;
        int startY = motion == Motion.MOVING ? nextRow : row;
        path.clear();
        pathCooldown = PATH_COOLDOWN;
        int w = map.getWidth();
        int h = map.getHeight();
        if (endX < 0 || endY < 0 || endX >= w || endY >= h) return;
        int[][] depth = depthMap(map);
        int[][] costs = new int[h][w];
        boolean[][] visited = new boolean[h][w];
        boolean[][] added = new boolean[h][w];
        for (int y = 0; y < h; y++) java.util.Arrays.fill(costs[y], MAX_COST);
        List<int[]> cells = new ArrayList<>();
        cells.add(new int[]{startX, startY});
        costs[startY][startX] = 0;
        added[startY][startX] = true;
        while (true) {
            int best = -1;
            int min = MAX_COST;
            for (int i = 0; i < cells.size(); i++) {
                int[] c = cells.get(i);
                if (visited[c[1]][c[0]] || costs[c[1]][c[0]] >= min) continue;
                min = costs[c[1]][c[0]];
                best = i;
            }
            if (best == -1) break;
            int[] p = cells.get(best);
            if (p[0] == endX && p[1] == endY) break;
            visited[p[1]][p[0]] = true;
            for (int i = 0; i < 4; i++) {
                int x = p[0] + OFF_X[i];
                int y = p[1] + OFF_Y[i];
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                int cost = Math.min(MAX_COST, costs[p[1]][p[0]] + depth[y][x]);
                if (costs[y][x] > cost) costs[y][x] = cost;
                if (added[y][x]) continue;
                cells.add(new int[]{x, y});
                added[y][x] = true;
            }
        }
        if (costs[endY][endX] >= MAX_COST) return;
        int currentCost = costs[endY][endX];
        int cx = endX;
        int cy = endY;
        List<int[]> turns = new ArrayList<>();
        turns.add(new int[]{cx, cy});
        while (currentCost != 0) {
            int ox = cx;
            int oy = cy;
            for (int i = 0; i < 4; i++) {
                int x = ox + OFF_X[i];
                int y = oy + OFF_Y[i];
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                if (costs[y][x] < currentCost) {
                    currentCost = costs[y][x];
                    cx = x;
                    cy = y;
                }
            }
            if (cx == ox && cy == oy) break;
            turns.add(new int[]{cx, cy});
        }
        // turns: đích → xuất phát; bỏ ô xuất phát, phần tử cuối là ô kế tiếp.
        turns.remove(turns.size() - 1);
        path.addAll(turns);
    }

    /** Giá vượt mỗi ô: gạch theo độ bền (1 + (10 - id) / 2), bụi 2, bê tông/thép/nước không qua. */
    private static int[][] depthMap(IMap map) {
        int w = map.getWidth();
        int h = map.getHeight();
        int[][] depth = new int[h][w];
        MapDefinition md = map instanceof MapDefinition ? (MapDefinition) map : null;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                TileType tile = map.getTileAt(x, y);
                int cost = 1;
                switch (tile) {
                    case WALL: {
                        boolean boundary = md != null && md.isBoundary(x, y);
                        Wall wall = md != null ? md.wallAt(x, y) : null;
                        int hp = wall != null ? Math.min(4, wall.getHitPoints()) : 4;
                        int id = 1 + 2 * (4 - hp);
                        cost = boundary ? MAX_COST : 1 + (10 - id) / 2;
                        break;
                    }
                    case BUSH:
                        cost = 2;
                        break;
                    case WHITE_WALL:
                    case STONE_WALL:
                    case WATER:
                        cost = MAX_COST;
                        break;
                    default:
                        break;
                }
                depth[y][x] = cost;
            }
        }
        return depth;
    }

    // ===== Occupancy (teamsGrid) =====
    private static boolean isViewWall(IMap map, int x, int y) {
        TileType tile = map.getTileAt(x, y);
        return tile == TileType.WALL || tile == TileType.WHITE_WALL
                || tile == TileType.BUSH || tile == TileType.STONE_WALL;
    }

    private boolean isFree(AITank self, GameWorld world, IMap map, int x, int y) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return false;
        if (!map.isWalkable(x, y)) return false;
        return !isOccupied(self, world, map, x, y);
    }

    private static boolean isOccupied(AITank self, GameWorld world, IMap map, int x, int y) {
        return occupantAt(self, world, map, x, y) != null;
    }

    private static boolean hasHostileAt(AITank self, GameWorld world, IMap map, int x, int y) {
        ITank occupant = occupantAt(self, world, map, x, y);
        return occupant != null && self.isHostileTo(occupant);
    }

    private static ITank occupantAt(AITank self, GameWorld world, IMap map, int x, int y) {
        int ts = map.getTileSize();
        Rectangle cell = new Rectangle(x * ts + 1, y * ts + 1, ts - 2, ts - 2);
        for (ITank t : world.getAllTanks()) {
            if (t == self || !t.isAlive()) continue;
            if (t instanceof AITank && !((AITank) t).isTurret()) {
                if (((AITank) t).getController().occupies(x, y)) return t;
                continue;
            }
            if (t instanceof com.tankzors.client.gameplay.entity.Tank
                    && ((com.tankzors.client.gameplay.entity.Tank) t).getBoundingBox().intersects(cell)) {
                return t;
            }
        }
        return null;
    }

    private static PlayerTank nearestPlayer(AITank self, GameWorld world) {
        PlayerTank best = null;
        double bestDist = Double.MAX_VALUE;
        for (ITank t : world.getAllTanks()) {
            if (!(t instanceof PlayerTank) || !t.isAlive()) continue;
            double d = Math.hypot(t.getPosition().getX() - self.getPosition().getX(),
                    t.getPosition().getY() - self.getPosition().getY());
            if (d < bestDist) {
                bestDist = d;
                best = (PlayerTank) t;
            }
        }
        return best;
    }

    private static int cellOf(double pixel, int ts) {
        return (int) Math.floor(pixel / ts);
    }

    /** Quy ước client: 0 = lên, 90 = phải, 180 = xuống, 270 = trái. */
    static double rotationOf(int dx, int dy) {
        if (dx == 1) return 90;
        if (dx == -1) return 270;
        if (dy == 1) return 180;
        return 0;
    }

    static int[] cardinal(double rotation) {
        int quadrant = (int) Math.floor((((rotation % 360) + 360) % 360 + 45) / 90) % 4;
        switch (quadrant) {
            case 1: return new int[]{1, 0};
            case 2: return new int[]{0, 1};
            case 3: return new int[]{-1, 0};
            default: return new int[]{0, -1};
        }
    }

    // Truy cập cho test.
    AiState aiState() { return aiState; }
    Motion motion() { return motion; }
    int col() { return col; }
    int row() { return row; }
    int pathLength() { return path.size(); }
}
