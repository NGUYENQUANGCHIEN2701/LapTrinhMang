package com.tankzors.client.ui;

import com.badlogic.gdx.Input;
import com.tankzors.client.shop.PlayerShopState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopOverlayTest {
    @Test void retryKeyIsAvailableWhileShopConsumesKeyboardInput() {
        int[] retries = {0};
        ShopOverlay shop = new ShopOverlay(null, null, new PlayerShopState(), new ShopOverlay.ShopListener() {
            public void onShopClosed() {}
            public void onShopStateChanged() {}
            public void retrySave() { retries[0]++; }
        });
        shop.show();
        assertTrue(shop.handleKeyDown(Input.Keys.R));
        assertEquals(1, retries[0]);
        assertTrue(shop.isVisible());
    }
}
