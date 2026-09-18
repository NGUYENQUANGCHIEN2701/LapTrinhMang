-- V4__player_totals.sql - Campaign rewards debriefing.
-- - player_totals: tổng stars/coins mỗi playerId (chỉ VICTORY được credit, clap idempotent bởi match_result.run_id UNIQUE).
-- - match_result: lưu stars_earned/coins_earned cho mỗi run (history/display).
-- Không sửa V1..V3 đã apply; cột mới nullable/default để tương thích row cũ.

CREATE TABLE IF NOT EXISTS player_totals (
    player_id    VARCHAR(80)  PRIMARY KEY,
    total_stars  INTEGER      NOT NULL DEFAULT 0,
    total_coins  INTEGER      NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cột mới (nullable/default để cũ row cũ vẫn hợp lệ).
ALTER TABLE match_result ADD COLUMN stars_earned INTEGER DEFAULT 0;
ALTER TABLE match_result ADD COLUMN coins_earned INTEGER DEFAULT 0;
