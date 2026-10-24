package com.tankzors.client.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;

/** Shared hit targets and keyboard choices for the retry confirmation. */
public final class DefeatPrompt {
    public final Rectangle retry = new Rectangle();
    public final Rectangle back = new Rectangle();
    public int selected;

    public void layout(float width, float height) {
        float size = Math.min(108f, Math.min(width * .16f, height * .19f));
        float gap = Math.min(150f, width * .16f);
        float y = height * .49f;
        retry.set(width / 2 - gap / 2 - size, y, size, size);
        back.set(width / 2 + gap / 2, y, size, size);
    }

    /** 1: retry, 2: campaigns, 0: keep prompt open. */
    public int key(int key) {
        if (key == Input.Keys.R) return 1;
        if (key == Input.Keys.ESCAPE) return 2;
        if (key == Input.Keys.ENTER || key == Input.Keys.SPACE) return selected + 1;
        if (key == Input.Keys.LEFT || key == Input.Keys.A) selected = 0;
        if (key == Input.Keys.RIGHT || key == Input.Keys.D) selected = 1;
        if (key == Input.Keys.TAB) selected = 1 - selected;
        return 0;
    }

    public int touch(float x, float y) {
        return retry.contains(x, y) ? 1 : back.contains(x, y) ? 2 : 0;
    }
}
