-- V6__player_shop_state.sql - Lưu trữ trạng thái cửa hàng, túi đồ và cây công nghệ của player
CREATE TABLE IF NOT EXISTS player_shop_state (
    player_id       VARCHAR(80) PRIMARY KEY,
    research_json   TEXT        NOT NULL DEFAULT '{}',
    inventory_json  TEXT        NOT NULL DEFAULT '{}',
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

