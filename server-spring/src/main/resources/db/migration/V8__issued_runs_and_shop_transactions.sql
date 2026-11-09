ALTER TABLE player_totals ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE game_run (
    run_id VARCHAR(120) PRIMARY KEY,
    player_id VARCHAR(80) NOT NULL,
    level_id VARCHAR(30) NOT NULL,
    map_version VARCHAR(60) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    claimed_pickups TEXT NOT NULL DEFAULT '[]',
    shop_floor TEXT NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_game_run_player ON game_run(player_id);

CREATE TABLE shop_request (
    request_id VARCHAR(201) PRIMARY KEY,
    player_id VARCHAR(80) NOT NULL,
    run_id VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL
);
CREATE INDEX idx_shop_request_player ON shop_request(player_id);

UPDATE player_shop_state SET inventory_json = '{"0":99,"19":3,"17":2}' WHERE inventory_json = '{}';
