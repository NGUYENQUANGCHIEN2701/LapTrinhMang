# Phase 1: Networked Movement Skeleton - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-13
**Phase:** 1-networked-movement-skeleton
**Areas discussed:** Kiểu điều khiển & di chuyển, Cảm giác di chuyển, Kích thước/bố cục sân đấu

---

## Kiểu điều khiển & di chuyển (Control Scheme & Rotation)

| Option | Description | Selected |
|--------|-------------|----------|
| Cổ điển Battle City (Recommended) | Pressing a direction rotates the tank body and moves it that way; firing always follows body direction | ✓ |
| Twin-stick (WASD + chuột) | WASD moves freely, mouse aims turret independently of movement direction | |

**User's choice:** Cổ điển Battle City
**Notes:** —

| Option | Description | Selected |
|--------|-------------|----------|
| 4 hướng (Recommended) | Up/down/left/right only | ✓ |
| 8 hướng | Adds diagonals | |

**User's choice:** 4 hướng

---

## Cảm giác di chuyển (Movement Feel)

| Option | Description | Selected |
|--------|-------------|----------|
| Tự do, pixel-by-pixel (Recommended) | Continuous coordinate movement, no grid snapping | ✓ |
| Snap theo ô lưới (grid) | Discrete tile-based movement, like original Battle City | |

**User's choice:** Tự do, pixel-by-pixel

| Option | Description | Selected |
|--------|-------------|----------|
| Để Claude chọn số hợp lý (Recommended) | Claude picks tank/bullet speed values, tunes after testing | ✓ |
| Tôi muốn nói rõ | User specifies exact speeds | |

**User's choice:** Để Claude chọn số hợp lý

---

## Kích thước/bố cục sân đấu (Arena Size & Layout)

| Option | Description | Selected |
|--------|-------------|----------|
| Nhỏ-vừa, toàn bộ trên 1 màn hình (Recommended) | ~800x600px, no camera scroll | ✓ |
| Rộng hơn màn hình, có camera theo tank | Larger arena with camera following the tank | |

**User's choice:** Nhỏ-vừa, toàn bộ trên 1 màn hình

| Option | Description | Selected |
|--------|-------------|----------|
| 2 góc đối diện (Recommended) | Tanks spawn diagonally opposite each other | ✓ |
| Vị trí cố định khác | User describes a specific alternative | |

**User's choice:** 2 góc đối diện

---

## Claude's Discretion

- Exact tank/bullet speed numbers
- Exact arena pixel dimensions (within "fits on one screen, no scrolling")
- Visual placeholder style for tanks/arena at this skeleton stage
- Message protocol envelope shape, tick rate, concurrency model (research already recommends specific defaults — Javalin, ~20-30Hz ScheduledExecutorService, single-threaded executor per match)

## Deferred Ideas

None — discussion stayed entirely within Phase 1 scope (movement sync only). Walls, shooting/ricochet, HP, accounts are already scoped to later phases per ROADMAP.md.
