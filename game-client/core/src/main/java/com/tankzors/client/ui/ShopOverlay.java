package com.tankzors.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.audio.AudioManager;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ResearchTech;
import com.tankzors.client.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện màn hình Cửa Hàng / Xưởng Vũ Khí (Workshop & Arsenal) trong game LibGDX.
 * Hoạt động trực tiếp trên client (Lựa chọn A), phong cách retro military chuẩn Tankzors Lux.
 */
public class ShopOverlay {

    public interface ShopListener {
        void onShopClosed();
        void onShopStateChanged();
        default void onShopVisibilityChanged(boolean visible) {}
        default void onItemBought(ShopItem item) {}
        default void onItemSold(ShopItem item) {}
        default int missingTankHp() { return 0; }
        default boolean repairTank() { return false; }
    }

    public enum Tab {
        AMMO("1. DAN DUOC"),
        DEFENSE("2. PHONG THU & HO TRO"),
        RESEARCH("3. NGHIEN CUU");

        private final String label;
        Tab(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final AssetRepository assets;
    private final AudioManager audio;
    private final PlayerShopState shopState;
    private final ShopListener listener;

    private boolean visible = false;
    private Tab activeTab = Tab.AMMO;
    private float scrollY = 0f;

    private final GlyphLayout layout = new GlyphLayout();
    private final List<ClickArea> clickAreas = new ArrayList<>();

    private static class ClickArea {
        final Rectangle bounds;
        final Runnable action;
        ClickArea(float x, float y, float w, float h, Runnable action) {
            this.bounds = new Rectangle(x, y, w, h);
            this.action = action;
        }
    }

    public ShopOverlay(AssetRepository assets, AudioManager audio,
                       PlayerShopState shopState, ShopListener listener) {
        this.assets = assets;
        this.audio = audio;
        this.shopState = shopState;
        this.listener = listener;
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        if (visible) return;
        visible = true;
        scrollY = 0f;
        if (audio != null) {
            audio.playUiWindow();
            audio.playBriefingMusic();
        }
        if (listener != null) listener.onShopVisibilityChanged(true);
    }

    public void hide() {
        if (!visible) return;
        visible = false;
        if (audio != null) {
            audio.playUiWindow();
            audio.stopMusic();
        }
        if (listener != null) {
            listener.onShopClosed();
            listener.onShopVisibilityChanged(false);
        }
    }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    public boolean handleTouchDown(float screenX, float screenY) {
        if (!visible) return false;
        for (ClickArea ca : clickAreas) {
            if (ca.bounds.contains(screenX, screenY)) {
                if (audio != null) audio.playUiClick();
                ca.action.run();
                if (listener != null) listener.onShopStateChanged();
                return true;
            }
        }
        return true; // Chặn click xuyên qua gameplay bên dưới
    }

    public boolean handleScrolled(float amountY) {
        if (!visible) return false;
        scrollY = Math.max(0, scrollY + amountY * 30f);
        return true;
    }

    public boolean handleKeyDown(int keycode) {
        if (!visible) {
            if (keycode == Input.Keys.B) {
                show();
                return true;
            }
            return false;
        }

        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.B) {
            hide();
            return true;
        }
        if (keycode == Input.Keys.NUM_1) {
            activeTab = Tab.AMMO;
            scrollY = 0;
            return true;
        }
        if (keycode == Input.Keys.NUM_2) {
            activeTab = Tab.DEFENSE;
            scrollY = 0;
            return true;
        }
        if (keycode == Input.Keys.NUM_3) {
            activeTab = Tab.RESEARCH;
            scrollY = 0;
            return true;
        }
        return true;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        if (!visible) return;

        clickAreas.clear();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // 1. Phủ mờ màn hình nền
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.78f);
        shapes.rect(0, 0, screenW, screenH);

        // Khung chính Panel Cửa Hàng
        float winW = Math.min(760f, screenW - 30f);
        float winH = Math.min(550f, screenH - 30f);
        float winX = (screenW - winW) / 2f;
        float winY = (screenH - winH) / 2f;

        // Khung viền và nền bảng kim loại tối
        shapes.setColor(0.10f, 0.12f, 0.15f, 0.98f);
        shapes.rect(winX, winY, winW, winH);

        // Viền trên Header
        shapes.setColor(0.18f, 0.22f, 0.28f, 1f);
        shapes.rect(winX, winY, winW, 46);

        // Viền ngoài màu vàng đồng
        shapes.setColor(0.85f, 0.65f, 0.20f, 0.85f);
        shapes.rect(winX, winY, winW, 2);
        shapes.rect(winX, winY + winH - 2, winW, 2);
        shapes.rect(winX, winY, 2, winH);
        shapes.rect(winX + winW - 2, winY, 2, winH);

        // Viền ngăn Tab Bar
        shapes.setColor(0.25f, 0.30f, 0.38f, 1f);
        shapes.rect(winX, winY + 84, winW, 2);
        shapes.end();

        // 2. Vẽ Text Header & Nút Tab
        batch.begin();
        font.setColor(Color.GOLD);
        font.draw(batch, "TANKZORS ARSENAL & WORKSHOP", winX + 16, winY + 16);

        // Ví tiền ở Header
        String walletText = "COINS: " + shopState.getCoins() + "  |  STARS: " + shopState.getStars();
        font.setColor(Color.WHITE);
        font.draw(batch, walletText, winX + winW - 270, winY + 16);

        // Nút Đóng [X]
        font.setColor(Color.RED);
        font.draw(batch, "[X]", winX + winW - 35, winY + 16);
        batch.end();

        clickAreas.add(new ClickArea(winX + winW - 45, winY + 8, 35, 30, this::hide));

        // Vẽ 3 Tabs
        drawTabs(batch, shapes, font, winX, winY + 48, winW);

        // Nội dung từng Tab
        float contentX = winX + 12;
        float contentY = winY + 92;
        float contentW = winW - 24;
        float contentH = winH - 146;

        if (activeTab == Tab.AMMO) {
            renderAmmoTab(batch, shapes, font, contentX, contentY, contentW, contentH);
        } else if (activeTab == Tab.DEFENSE) {
            renderDefenseTab(batch, shapes, font, contentX, contentY, contentW, contentH);
        } else if (activeTab == Tab.RESEARCH) {
            renderResearchTab(batch, shapes, font, contentX, contentY, contentW, contentH);
        }

        // Bottom Bar (Sửa xe & Vào trận)
        drawBottomBar(batch, shapes, font, winX, winY + winH - 46, winW);
    }

    private void drawTabs(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                          float x, float y, float w) {
        Tab[] tabs = Tab.values();
        float tabW = (w - 24) / tabs.length;

        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            float tx = x + 12 + i * tabW;
            boolean active = (t == activeTab);

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(active ? new Color(0.25f, 0.45f, 0.65f, 1f) : new Color(0.14f, 0.17f, 0.22f, 1f));
            shapes.rect(tx + 2, y + 2, tabW - 4, 30);
            shapes.end();

            batch.begin();
            font.setColor(active ? Color.YELLOW : Color.LIGHT_GRAY);
            font.draw(batch, t.getLabel(), tx + 16, y + 10);
            batch.end();

            final Tab selectedTab = t;
            clickAreas.add(new ClickArea(tx, y, tabW, 34, () -> {
                this.activeTab = selectedTab;
                this.scrollY = 0f;
            }));
        }
    }

    private void renderAmmoTab(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                               float x, float y, float w, float h) {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : ShopItem.values()) {
            if (item.getCategory() == ShopItem.Category.BASIC_AMMO
                    || item.getCategory() == ShopItem.Category.SPECIAL_AMMO) {
                items.add(item);
            }
        }
        renderItemList(batch, shapes, font, x, y, w, h, items);
    }

    private void renderDefenseTab(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                                  float x, float y, float w, float h) {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : ShopItem.values()) {
            if (item.getCategory() == ShopItem.Category.DEFENSE
                    || item.getCategory() == ShopItem.Category.SUPPORT
                    || item.getCategory() == ShopItem.Category.ALLIES) {
                items.add(item);
            }
        }
        renderItemList(batch, shapes, font, x, y, w, h, items);
    }

    private void renderItemList(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                                float x, float y, float w, float h, List<ShopItem> items) {
        float rowH = 48f;
        float startY = y - scrollY;

        for (ShopItem item : items) {
            float rowY = startY;
            startY += rowH;

            // Kiểm tra xem hàng có nằm trong vùng nhìn thấy không
            if (rowY + rowH < y || rowY > y + h) continue;

            boolean unlocked = shopState.isItemUnlocked(item);
            boolean canBuy = shopState.canBuy(item);
            boolean canSell = shopState.canSell(item);
            int count = shopState.getItemCount(item.getId());
            int limit = shopState.getEffectiveLimit(item);
            int price = shopState.getEffectivePrice(item);

            // Nền từng hàng
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.12f, 0.15f, 0.19f, 0.9f);
            shapes.rect(x, rowY + 2, w, rowH - 4);

            // Nút Mua [+]
            float buyBtnX = x + w - 75;
            shapes.setColor(canBuy ? new Color(0.18f, 0.55f, 0.22f, 1f) : new Color(0.25f, 0.28f, 0.32f, 0.7f));
            shapes.rect(buyBtnX, rowY + 8, 32, 28);

            // Nút Bán [-]
            float sellBtnX = x + w - 38;
            shapes.setColor(canSell ? new Color(0.65f, 0.25f, 0.20f, 1f) : new Color(0.25f, 0.28f, 0.32f, 0.7f));
            shapes.rect(sellBtnX, rowY + 8, 32, 28);
            shapes.end();

            // Vẽ Icon và Thông tin Item
            batch.begin();
            TextureRegion icon = assets.getIcon(item.getId());
            if (icon != null) {
                batch.draw(icon, x + 8, rowY + 6, 36, 36);
            }

            // Tên và mô tả
            font.setColor(unlocked ? Color.WHITE : Color.GRAY);
            String title = item.getDisplayName() + (item.getPackSize() > 1 ? " (" + item.getPackSize() + "x)" : "");
            font.draw(batch, title, x + 52, rowY + 12);

            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, item.getDescription(), x + 52, rowY + 28);

            // Số lượng & Giá
            font.setColor(count >= limit ? Color.ORANGE : Color.GREEN);
            String countStr = count + "/" + limit;
            font.draw(batch, countStr, x + w - 210, rowY + 16);

            font.setColor(Color.GOLD);
            font.draw(batch, price + " C", x + w - 145, rowY + 16);

            if (!unlocked) {
                font.setColor(Color.CORAL);
                font.draw(batch, "[KHOA]", x + w - 210, rowY + 30);
            }

            // Ký hiệu trên nút Mua / Bán
            font.setColor(Color.WHITE);
            font.draw(batch, "+", buyBtnX + 11, rowY + 14);
            font.draw(batch, "-", sellBtnX + 12, rowY + 14);
            batch.end();

            // Đăng ký vùng bấm
            if (canBuy) {
                clickAreas.add(new ClickArea(buyBtnX, rowY + 8, 32, 28, () -> {
                    if (shopState.buy(item) && listener != null) listener.onItemBought(item);
                }));
            }
            if (canSell) {
                clickAreas.add(new ClickArea(sellBtnX, rowY + 8, 32, 28, () -> {
                    if (shopState.sell(item) && listener != null) listener.onItemSold(item);
                }));
            }
        }
    }

    private void renderResearchTab(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                                   float x, float y, float w, float h) {
        float rowH = 48f;
        float startY = y - scrollY;

        for (ResearchTech tech : ResearchTech.values()) {
            float rowY = startY;
            startY += rowH;

            if (rowY + rowH < y || rowY > y + h) continue;

            int currentLvl = shopState.getResearchLevel(tech.getId());
            boolean canUpgrade = shopState.canUpgradeResearch(tech);
            boolean maxed = currentLvl >= tech.getMaxLevel();
            int starPrice = tech.getPriceForLevel(currentLvl);

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.12f, 0.15f, 0.19f, 0.9f);
            shapes.rect(x, rowY + 2, w, rowH - 4);

            // Nút Nâng cấp
            float btnX = x + w - 110;
            shapes.setColor(canUpgrade ? new Color(0.20f, 0.45f, 0.70f, 1f) : new Color(0.25f, 0.28f, 0.32f, 0.7f));
            shapes.rect(btnX, rowY + 8, 100, 28);
            shapes.end();

            batch.begin();
            font.setColor(maxed ? Color.GOLD : Color.CYAN);
            font.draw(batch, tech.getDisplayName(), x + 16, rowY + 12);

            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, tech.getDescription(), x + 16, rowY + 28);

            // Cấp độ
            font.setColor(maxed ? Color.GOLD : Color.YELLOW);
            String lvlStr = "CAP: " + currentLvl + "/" + tech.getMaxLevel();
            font.draw(batch, lvlStr, x + w - 210, rowY + 16);

            // Nút nâng cấp text
            font.setColor(Color.WHITE);
            String btnText = maxed ? "TOI DA" : ("UP (" + starPrice + " S)");
            font.draw(batch, btnText, btnX + 10, rowY + 14);
            batch.end();

            if (canUpgrade) {
                clickAreas.add(new ClickArea(btnX, rowY + 8, 100, 28, () -> {
                    if (shopState.upgradeResearch(tech) && listener != null) listener.onShopStateChanged();
                }));
            }
        }
    }

    private void drawBottomBar(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                               float x, float y, float w) {
        // Nút Sửa chữa xe tăng
        float repairBtnW = 200f;
        float repairBtnX = x + 16;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        int missingHp = listener != null ? listener.missingTankHp() : 0;
        int repairCost = shopState.getRepairTankCost(missingHp);
        boolean canRepair = missingHp > 0 && shopState.getCoins() >= repairCost;
        shapes.setColor(canRepair ? new Color(0.22f, 0.32f, 0.42f, 1f) : new Color(0.25f, 0.28f, 0.32f, 0.7f));
        shapes.rect(repairBtnX, y + 6, repairBtnW, 32);

        // Nút Tiếp tục / Chiến đấu
        float battleBtnW = 180f;
        float battleBtnX = x + w - battleBtnW - 16;
        shapes.setColor(new Color(0.18f, 0.60f, 0.25f, 1f));
        shapes.rect(battleBtnX, y + 6, battleBtnW, 32);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "[ SUA XE " + missingHp + " HP - " + repairCost + " C ]", repairBtnX + 8, y + 14);
        font.setColor(Color.YELLOW);
        font.draw(batch, "[ CHIEN DAU (ESC) ]", battleBtnX + 16, y + 14);
        batch.end();

        if (canRepair) clickAreas.add(new ClickArea(repairBtnX, y + 6, repairBtnW, 32,
                () -> { if (listener != null) listener.repairTank(); }));

        clickAreas.add(new ClickArea(battleBtnX, y + 6, battleBtnW, 32, this::hide));
    }
}
