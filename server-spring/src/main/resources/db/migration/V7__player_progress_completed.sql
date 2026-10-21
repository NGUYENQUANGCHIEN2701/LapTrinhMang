-- V7__player_progress_completed.sql - Mỗi màn campaign chỉ chơi một lần.
-- Thắng màn N thì màn N bị khoá (completed) và màn N+1 được mở.

ALTER TABLE player_progress ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;

-- Dữ liệu cũ: mọi màn thấp hơn màn cao nhất đã mở coi như đã hoàn thành
-- (campaign_1 < campaign_2 < campaign_3 theo thứ tự chuỗi).
UPDATE player_progress p SET completed = TRUE
WHERE EXISTS (
    SELECT 1 FROM player_progress q
    WHERE q.session_id = p.session_id AND q.unlocked_level > p.unlocked_level
);
