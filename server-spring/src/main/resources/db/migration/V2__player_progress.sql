-- V2__player_progress.sql - Persist campaign unlock progression per player session.
-- Phase 5+: thay thế approach in-memory bằng PostgreSQL persistent.

CREATE TABLE player_progress (
    session_id    VARCHAR(80)  NOT NULL,
    unlocked_level VARCHAR(30) NOT NULL,
    PRIMARY KEY (session_id, unlocked_level)
);

-- Default row mới sẽ được insert từ service layer khi session đầu tiên đăng nhập
-- (campaign_1 luôn unlocked).
