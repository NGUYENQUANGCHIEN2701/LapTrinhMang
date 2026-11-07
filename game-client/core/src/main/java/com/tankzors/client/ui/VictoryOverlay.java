package com.tankzors.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.model.GameStateSnapshot;

/** A scrollable mission report with fixed, mouse/keyboard accessible actions. */
public final class VictoryOverlay implements Disposable {
    private final BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/vietnamese-font.fnt"), true);
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle continueButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();
    private final Color gold = new Color(1f, .79f, .28f, 1f);
    private final Color muted = new Color(.76f, .66f, .60f, 1f);
    private float scroll, maxScroll;
    private int selected;
    private GameStateSnapshot previous;

    public VictoryOverlay() {
        font.getRegion().getTexture().setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
    }

    /** 1 = continue via workshop, 2 = campaigns, 0 = no action. */
    public int key(int key) {
        if (key == Input.Keys.ESCAPE) return 2;
        if (key == Input.Keys.ENTER || key == Input.Keys.SPACE) return selected + 1;
        if (key == Input.Keys.TAB || key == Input.Keys.LEFT || key == Input.Keys.RIGHT) selected = 1 - selected;
        if (key == Input.Keys.DOWN || key == Input.Keys.S) scroll(1);
        if (key == Input.Keys.UP || key == Input.Keys.W) scroll(-1);
        if (key == Input.Keys.PAGE_DOWN) scroll(6);
        if (key == Input.Keys.PAGE_UP) scroll(-6);
        return 0;
    }

    public int touch(float x, float y) {
        if (continueButton.contains(x, y)) return 1;
        if (backButton.contains(x, y)) return 2;
        return 0;
    }

    public void scroll(float amount) { scroll = Math.max(0, Math.min(maxScroll, scroll + amount * 36)); }

    public void render(GameStateSnapshot state, SpriteBatch batch, ShapeRenderer shapes, AssetRepository assets) {
        if (previous == null) {
            scroll = 0;
            selected = 0;
        }
        previous = state;
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float scale = Math.max(.62f, Math.min(1f, w / 1000f));
        float x = Math.max(24, (w - 900 * scale) / 2), cw = w - x * 2;
        float footer = 106 * scale, top = 32 * scale;
        float viewBottom = Math.max(top + 60, h - footer - 12);
        float contentHeight = 545 * scale;
        maxScroll = Math.max(0, contentHeight - (viewBottom - top));
        scroll = Math.min(scroll, maxScroll);
        continueButton.set(x, h - footer + 12 * scale, cw * .60f, 48 * scale);
        backButton.set(x + cw * .63f, continueButton.y, cw * .37f, 48 * scale);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, w, h, new Color(.19f,.035f,.025f,1), new Color(.19f,.035f,.025f,1),
                new Color(.36f,.055f,.025f,1), new Color(.36f,.055f,.025f,1));
        shapes.setColor(.55f,.28f,.12f,1);
        shapes.rect(x, 12 * scale, cw, 2 * scale);
        shapes.end();

        // Clip only the report: action buttons remain visible while scrolling.
        // Clip from below the rule, not from `top`: y is the cap line, Vietnamese marks sit above it.
        float clipTop = 14 * scale;
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int)(h - viewBottom), (int)w, Math.max(1, (int)(viewBottom - clipTop)));
        float y = top - scroll;
        batch.begin();
        text(batch, "NHIỆM VỤ HOÀN THÀNH", x, y, cw, 1.04f * scale, gold);
        y += 53 * scale;
        text(batch, "Xuất sắc! Bạn đã hoàn thành nhiệm vụ.", x, y, cw, .72f * scale, Color.WHITE);
        y += 42 * scale;
        text(batch, "Hãy ghé xưởng để bổ sung đạn, mua trang bị và nghiên cứu nâng cấp bằng Xu, Sao bạn tích lũy được. Trang bị còn lại sẽ theo bạn sang nhiệm vụ tiếp theo.",
                x, y, cw, .62f * scale, muted);
        y += 87 * scale;
        text(batch, "BÁO CÁO CHIẾN ĐẤU", x, y, cw, .75f * scale, gold);
        y += 48 * scale;
        for (String[] row : VictorySummary.rows(state)) {
            text(batch, row[0], x + 12 * scale, y, cw * .67f, .65f * scale, muted);
            font.getData().setScale(.70f * scale);
            font.setColor(Color.WHITE);
            font.draw(batch, row[1], x + cw * .68f, y, cw * .30f, Align.right, false);
            y += 33 * scale;
        }
        batch.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.12f,.025f,.02f,1);
        shapes.rect(0, h - footer, w, footer);
        shapes.setColor(.55f,.28f,.12f,1);
        shapes.rect(x, h - footer, cw, 1);
        button(shapes, continueButton, selected == 0);
        button(shapes, backButton, selected == 1);
        if (maxScroll > 0) {
            shapes.setColor(.40f,.16f,.10f,1);
            shapes.rect(w - 10, top, 3, viewBottom - top);
            float thumb = Math.max(24, (viewBottom - top) * (viewBottom - top) / contentHeight);
            shapes.setColor(gold);
            shapes.rect(w - 10, top + scroll / maxScroll * (viewBottom - top - thumb), 3, thumb);
        }
        shapes.end();
        batch.begin();
        centered(batch, "Tiếp tục", continueButton, .76f * scale, gold);
        centered(batch, "Quay lại", backButton, .76f * scale, Color.WHITE);
        batch.end();
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
    }

    private void button(ShapeRenderer shapes, Rectangle r, boolean active) {
        shapes.setColor(active ? gold : new Color(.45f,.25f,.18f,1));
        shapes.rect(r.x, r.y, r.width, r.height);
        shapes.setColor(active ? new Color(.32f,.12f,.07f,1) : new Color(.20f,.07f,.045f,1));
        shapes.rect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
    }
    private void centered(SpriteBatch batch, String value, Rectangle r, float size, Color color) {
        font.getData().setScale(size);
        layout.setText(font, value);
        text(batch, value, r.x, r.y + (r.height - layout.height) / 2, r.width, size, color, Align.center);
    }
    private void text(SpriteBatch batch, String value, float x, float y, float width, float size, Color color) {
        text(batch, value, x, y, width, size, color, Align.left);
    }
    private void text(SpriteBatch batch, String value, float x, float y, float width, float size, Color color, int align) {
        font.getData().setScale(size);
        font.setColor(color);
        font.draw(batch, value, x, y, width, align, true);
    }
    public void reset() { previous = null; }
    @Override public void dispose() { font.dispose(); }
}
