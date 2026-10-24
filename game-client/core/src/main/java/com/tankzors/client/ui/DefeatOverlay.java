package com.tankzors.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

/** Builds-style retry question drawn directly over the last battlefield. */
public final class DefeatOverlay implements Disposable {
    private final BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/vietnamese-font.fnt"), true);
    private final Texture okTexture = new Texture(Gdx.files.internal("gui/ts/ok.png"));
    private final Texture cancelTexture = new Texture(Gdx.files.internal("gui/ts/cancel.png"));
    private final TextureRegion ok = new TextureRegion(okTexture);
    private final TextureRegion cancel = new TextureRegion(cancelTexture);
    private final DefeatPrompt prompt = new DefeatPrompt();

    public DefeatOverlay() {
        ok.flip(false, true);
        cancel.flip(false, true);
        okTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cancelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public int key(int key) { return prompt.key(key); }
    public int touch(float x, float y) { return prompt.touch(x, y); }
    public void reset() { prompt.selected = 0; }

    public void render(SpriteBatch batch, ShapeRenderer shapes) {
        float width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        prompt.layout(width, height);
        float scale = Math.max(.5f, Math.min(1f, width / 1000f));
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, .38f);
        shapes.rect(0, 0, width, height);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(.92f * scale);
        font.setColor(0, 0, 0, .9f);
        float titleY = prompt.retry.y - 58 * scale;
        font.draw(batch, "Nhiệm vụ thất bại. Thử lại?", 2, titleY + 2, width, Align.center, false);
        font.setColor(1f, .17f, .10f, 1f);
        font.draw(batch, "Nhiệm vụ thất bại. Thử lại?", 0, titleY, width, Align.center, false);
        icon(batch, ok, prompt.retry, prompt.selected == 0);
        icon(batch, cancel, prompt.back, prompt.selected == 1);
        batch.end();
        font.setColor(Color.WHITE);
        font.getData().setScale(1);
    }

    private void icon(SpriteBatch batch, TextureRegion image, Rectangle bounds, boolean selected) {
        batch.setColor(1, 1, 1, selected ? .95f : .62f);
        batch.draw(image, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(Color.WHITE);
    }

    @Override public void dispose() {
        font.dispose();
        okTexture.dispose();
        cancelTexture.dispose();
    }
}
