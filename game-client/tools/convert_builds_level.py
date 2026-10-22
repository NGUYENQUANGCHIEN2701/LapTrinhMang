"""Chuyển một màn campaign Builds gốc (maps/en/NNN.json, vers 6) sang schema Tankzors.

Cách dùng:
    python convert_builds_level.py <builds_maps_dir> <level_index> <campaign_id>
Ví dụ (màn 2 gốc = 001.json):
    python convert_builds_level.py ../Builds/docs/html5/maps/en 1 campaign_2

Ghi đồng thời game-client/assets/maps/<id>.json và
server-spring/src/main/resources/maps/<id>.json.

Bảng giải mã dựa trên OldMap.parseMap trong kha.js của Builds:
  < 91        : nền (ground id)
  100..114    : (code-100)%5 → 0 base người chơi, 1 base địch, 2 ô trigger
  115..129    : 37 + (code-115)%5, 37 là cờ đích
  130..249    : tile = (code-130)%40+1 → 1-8 gạch, 9-10 bê tông, 11 khối đá,
                12-13 thép, 16 bụi, 22-31 ụ súng, 32-37 vật phẩm, 38-40 thùng đồ
  >= 250      : nền id (code-149)
Văn bản giữ ASCII vì font bitmap trong game không có dấu tiếng Việt.
"""
import json
import os
import sys

# Mã TileType phía client (TileType.java).
GROUND, WALL, WATER, SPAWN_PLAYER, SPAWN_ENEMY, WHITE_WALL, BUSH = 0, 1, 2, 3, 4, 5, 6
REPAIR, SPEED, FLAG = 7, 8, 9
FREEZE, STAR, COIN, BOX, LIFE, STONE_WALL = 10, 11, 12, 13, 14, 15

THEME = [1, 34, 56]
DIRECTIONS = {0: "DOWN", 1: "LEFT", 2: "UP", 3: "RIGHT"}
TURRET_ENEMY_HP = [8, 15, 8, 15, 15, 15, 20, 25, 35, 50]
DIFFICULTY = {1: "EASY", 2: "MEDIUM", 3: "HARD"}

# OldMap.getBoxItem: mã nội dung thùng → (item id, số lượng).
BOX_ITEMS = {0: (-1, 0), 1: (0, 30), 2: (1, 20), 3: (3, 20), 4: (2, 20), 5: (4, 10),
             6: (5, 10), 7: (10, 10), 8: (11, 5), 9: (12, 3), 10: (13, 2), 11: (14, 1),
             12: (6, 2), 13: (8, 1), 14: (7, 2), 15: (9, 1), 16: (25, 1), 17: (20, 3),
             18: (21, 1), 19: (-2, 1)}

# Bản dịch nội dung màn (không dấu). Khoá theo chỉ số màn gốc.
TEXTS = {
    1: {
        "name": "Màn 2 - Tường thành",
        "briefing": "Vai phim huu ich:\n"
                    "Q - chon dan / vat pham trong tran.\n"
                    "B - vao Workshop sau khi hoan thanh man.\n"
                    "Trong kho, ngoai dan con co bo tang toc, dong bang va bo sua xe.\n\n"
                    "O man nay se co mot xe tang dong minh ho tro ban.\n\n"
                    "Nhiem vu: tieu diet 6 xe tang dich.",
        "debriefing": "Xuat sac! Choi cung ban that vui.\n\n"
                      "Ve Workshop: trong khi choi ban tich luy Xu va Sao.\n"
                      "Xu dung de mua trang bi, Sao dung de nghien cuu vu khi moi.",
        "messages": ["Bam Q de chon vu khi va bo sua xe."],
    },
    2: {
        "name": "Màn 3 - Sa mạc",
        "briefing": "Nhiem vu: tieu diet toan bo xe tang va thap phao dich.\n\n"
                    "Luu y: thap phao rat nhay cam voi chat no.",
        "debriefing": "Tuyet voi!!! Chung ta da tien vao vung sa mac.\n\n"
                      "Tren cat, xe chay cham hon nhung hieu ung tang toc\n"
                      "keo dai gap doi.",
        "messages": ["Bam Q de chon thuoc no (Dynamite), Space de dat.",
                     "Ban co the nghien cuu cong nghe moi trong Workshop bang Sao.",
                     "Bam Q roi chon bo sua xe de sua nhanh."],
    },
}


def theme_of(code):
    """Nền theo theme mà kha.js gán cho các mã không phải ground thuần."""
    if code < 91:
        return code
    if code >= 250:
        return code - 149
    if 100 <= code < 115:
        return THEME[min(2, (code - 100) // 5)]
    if 115 <= code < 130:
        return THEME[min(2, (code - 115) // 5)]
    if 130 <= code < 250:
        return THEME[min(2, (code - 130) // 40)]
    return 1


def is_grass(a):
    return (1 <= a <= 10 or 15 <= a <= 23 or 53 <= a <= 55 or 100 <= a <= 102
            or 115 <= a <= 117 or 130 <= a <= 169 or 250 <= a <= 255)


def is_sand(a):
    return 56 <= a <= 60 or 110 <= a <= 112 or 125 <= a <= 127 or 210 <= a <= 249


def sand_edges(raw):
    """Viền cát/cỏ như OldMap.parseMap (chọn biến thể đầu thay cho Std.random)."""
    h, w = len(raw), len(raw[0])
    out = [row[:] for row in raw]

    def cell(x, y):
        return raw[y][x] if 0 <= x < w and 0 <= y < h else -1

    for y in range(h):
        for x in range(w):
            if raw[y][x] >= 91 or not is_sand(raw[y][x]):
                continue  # chỉ ô nền thuần mới ghi lại được vào visualGrid
            a, b = is_grass(cell(x, y - 1)), is_grass(cell(x, y + 1))
            c, d = is_grass(cell(x - 1, y)), is_grass(cell(x + 1, y))
            e, f = is_grass(cell(x - 1, y - 1)), is_grass(cell(x - 1, y + 1))
            g, hh = is_grass(cell(x + 1, y - 1)), is_grass(cell(x + 1, y + 1))
            if a and g and d: v = 70
            elif d and hh and b: v = 71
            elif c and f and b: v = 68
            elif c and e and a: v = 69
            elif a: v = 64
            elif b: v = 61
            elif c: v = 63
            elif d: v = 66
            elif e: v = 73
            elif g: v = 74
            elif f: v = 72
            elif hh: v = 75
            else: continue
            out[y][x] = v
    return out


def convert(src, level_index):
    p = src["parameters"]
    raw = src["map_arr"]
    h, w = len(raw), len(raw[0])
    texts = TEXTS[level_index]
    grid = [[GROUND] * w for _ in range(h)]
    player_bases, enemy_bases, trigger_cells, turrets, boxes = [], [], [], [], []
    buildings = []

    for y in range(h):
        for x in range(w):
            v = raw[y][x]
            if 91 <= v <= 94:
                kind = "reactor" if v <= 92 else "radar"
                team = 1 + (v - (91 if v <= 92 else 93))
                if team == 1 and not 50 <= p[25] <= 55:
                    team = -3
                buildings.append({"x": x, "y": y, "type": kind, "team": team,
                                  "hitPoints": (p[66] if p[66] > 0 else 3) if kind == "reactor" else (p[65] if p[65] > 0 else 7)})
            if v < 91 or v >= 250 or 91 <= v < 100:
                continue
            if v < 115:
                kind = (v - 100) % 5
                if kind == 0:
                    grid[y][x] = SPAWN_PLAYER
                    player_bases.append((x, y))
                elif kind == 1:
                    grid[y][x] = SPAWN_ENEMY
                    enemy_bases.append((x, y))
                elif kind == 2:
                    trigger_cells.append((x, y))
                continue
            if v < 130:
                if 37 + (v - 115) % 5 == 37:
                    grid[y][x] = FLAG
                continue
            t = (v - 130) % 40 + 1
            if t <= 8:
                grid[y][x] = WALL
            elif t in (9, 10, 12, 13):
                grid[y][x] = WHITE_WALL
            elif t == 11:
                grid[y][x] = STONE_WALL
            elif 14 <= t <= 18:
                grid[y][x] = BUSH
            elif 22 <= t <= 31:
                turret_id = t - 22
                if turret_id < 4:  # ụ súng cố định của phe địch
                    turret_type = max(0, p[18] - 1)
                    turrets.append({"x": x, "y": y, "direction": DIRECTIONS[turret_id],
                                    "range": p[21], "hitPoints": TURRET_ENEMY_HP[turret_type]})
            elif 32 <= t <= 37:
                grid[y][x] = {32: SPEED, 33: LIFE, 34: FREEZE, 35: REPAIR, 36: STAR, 37: COIN}[t]
            elif 38 <= t <= 40:
                grid[y][x] = BOX
                item_id, count = BOX_ITEMS.get(p[62 + min(t, 39) - 38], (0, 30))
                boxes.append({"x": x, "y": y, "itemId": item_id, "count": count})

    # Base địch: chỉ base có thứ tự (p[69..78]) mới spawn, lần lượt theo thứ hạng.
    total_enemies = sum(p[1:9])
    order = list(range(len(enemy_bases)))
    if p[69] != 0:
        order.sort(key=lambda i: p[69 + i] if p[69 + i] > 0 else 99)
    remaining = total_enemies
    wave_bases = []
    for i in order:
        count = min(p[32 + i], remaining)
        if count > 0:
            x, y = enemy_bases[i]
            wave_bases.append({"x": x, "y": y, "count": count})
            remaining -= count
    if remaining > 0 and wave_bases:  # OldMap.fixSpawnCounts dồn phần dư vào base
        wave_bases[-1]["count"] += remaining

    total_allies = sum(p[10:18])
    allies = None
    if total_allies > 0 and player_bases:
        x, y = player_bases[0]
        allies = {"maxOnField": p[9], "intervalSeconds": 1.0,
                  "followPlayer": p[85] == 1, "bases": [{"x": x, "y": y, "count": total_allies}],
                  "types": p[10:18], "level": p[23]}

    triggers = []
    messages = texts["messages"]
    for i, (x, y) in enumerate(trigger_cells):
        kind, param, _delay, _sens, flags = src["triggers"][i]
        entry = {"x": x, "y": y, "active": (flags & 1) != 0}
        if 21 <= kind <= 27:
            entry.update({"action": "TEXT", "text": messages[kind - 21],
                          "seconds": 2 if param == 0 else param})
        elif kind == 31:
            entry.update({"action": "PRESS", "target": param % 32 - 1})
        elif kind == 33:
            entry.update({"action": "ENABLE", "target": param - 1})
        else:
            entry.update({"action": "NONE"})
        triggers.append(entry)

    objective = {1: "DESTROY_TANKS", 60: "DESTROY_ALL"}.get(p[25], "DESTROY_TANKS")
    mission = {
        "objective": objective,
        "briefing": texts["briefing"],
        "debriefing": texts["debriefing"],
        "enemyWaves": {"maxOnField": p[0], "intervalSeconds": 1.0,
                       "difficulty": DIFFICULTY.get(p[23], "EASY"), "bases": wave_bases,
                       # Loại xe 0..7 theo teamCounts, cấp độ enemyDifficulty, enemyAIType & 8 = ngẫu nhiên.
                       "types": p[1:9], "level": p[23], "randomTypes": (p[24] & 8) != 0},
        "turrets": turrets,
        "buildings": buildings,
        "boxes": boxes,
        "triggers": triggers,
    }
    if allies:
        mission["allies"] = allies
    return grid, sand_edges(raw), mission, texts["name"]


def main():
    maps_dir, level_index, campaign_id = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    with open(os.path.join(maps_dir, "%03d.json" % level_index), encoding="utf-8") as fh:
        src = json.load(fh)
    grid, visual, mission, name = convert(src, level_index)

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    targets = [os.path.join(root, "assets", "maps", campaign_id + ".json"),
               os.path.join(root, "..", "server-spring", "src", "main", "resources", "maps",
                            campaign_id + ".json")]
    with open(targets[0], encoding="utf-8") as fh:
        current = json.load(fh)
    out = {
        "name": name,
        "difficulty": current.get("difficulty", "EASY"),
        "width": len(grid[0]),
        "height": len(grid),
        "tileSize": 40,
        "enemies": [],
        "rewards": current.get("rewards", {"stars": 3, "coins": 100}),
        "grid": grid,
        "visualGrid": visual,
        "mission": mission,
    }
    for path in targets:
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(out, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
        print("wrote", os.path.normpath(path))


if __name__ == "__main__":
    main()
