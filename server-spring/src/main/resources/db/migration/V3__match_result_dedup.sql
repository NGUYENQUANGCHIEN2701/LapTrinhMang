-- V3__match_result_dedup.sql
-- Bổ sung match_result cho client-reported results (Bước D):
-- - run_id UNIQUE để chống ghi trùng kết quả (idempotency).
-- - ai_kills, map_version, player_id (identity ứng dụng).
-- - payload_hash để phân biệt retry (cùng runId+payload -> trả đã lưu; cùng runId+payload khác -> 409).
-- Không sửa V1/V2 đã apply.

-- Cột mới (nullable để tương thích row cũ).
ALTER TABLE match_result ADD COLUMN run_id       VARCHAR(120);
ALTER TABLE match_result ADD COLUMN map_version  VARCHAR(60);
ALTER TABLE match_result ADD COLUMN ai_kills     INTEGER DEFAULT 0;
ALTER TABLE match_result ADD COLUMN player_id    VARCHAR(80);
ALTER TABLE match_result ADD COLUMN payload_hash VARCHAR(64);

-- Unique chống trùng: mỗi runId chỉ có 1 kết quả.
ALTER TABLE match_result ADD CONSTRAINT match_result_run_unique UNIQUE (run_id);

CREATE INDEX idx_match_result_player ON match_result (player_id);
