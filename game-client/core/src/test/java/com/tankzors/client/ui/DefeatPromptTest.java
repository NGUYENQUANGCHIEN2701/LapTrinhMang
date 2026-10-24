package com.tankzors.client.ui;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefeatPromptTest {
    @Test void retryAndExitHaveDistinctMouseTargetsAtDifferentViewportSizes() {
        for (float width : new float[]{360, 800, 1920}) {
            DefeatPrompt prompt = new DefeatPrompt();
            prompt.layout(width, 600);
            assertEquals(1, prompt.touch(prompt.retry.x + prompt.retry.width / 2, prompt.retry.y + 1));
            assertEquals(2, prompt.touch(prompt.back.x + prompt.back.width / 2, prompt.back.y + 1));
            assertEquals(0, prompt.touch(width / 2, prompt.retry.y));
            assertTrue(prompt.retry.x >= 0);
            assertTrue(prompt.back.x + prompt.back.width <= width);
        }
    }

    @Test void keyboardChoosesRetryOrCampaignWithoutOpeningWorkshop() {
        DefeatPrompt prompt = new DefeatPrompt();
        assertEquals(1, prompt.key(Input.Keys.ENTER));
        prompt.key(Input.Keys.RIGHT);
        assertEquals(2, prompt.key(Input.Keys.ENTER));
        prompt.key(Input.Keys.TAB);
        assertEquals(1, prompt.key(Input.Keys.SPACE));
        assertEquals(1, prompt.key(Input.Keys.R));
        assertEquals(2, prompt.key(Input.Keys.ESCAPE));
        assertEquals(0, prompt.key(Input.Keys.B));
    }
}
