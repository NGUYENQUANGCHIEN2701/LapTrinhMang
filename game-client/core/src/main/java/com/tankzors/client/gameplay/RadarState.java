package com.tankzors.client.gameplay;

/** Builds Radar: independent 60 Hz display clock; no simulation time is advanced. */
public final class RadarState {
    private boolean open, paused, showObjects;
    private int countdown;
    private double remainder;
    public boolean isOpen() { return open; }
    public boolean isPaused() { return paused; }
    public boolean showsObjects() { return showObjects; }
    public void open() { open = true; paused = false; }
    public void close() { open = false; paused = false; }
    public void togglePause() { if (open) paused = !paused; }
    public void reset() { close(); countdown = 0; remainder = 0; showObjects = false; }
    public void update(double seconds) {
        if (!open || paused || !Double.isFinite(seconds) || seconds <= 0) return;
        remainder += seconds * 60;
        int ticks = (int) (remainder + 1e-9);
        remainder -= ticks;
        for (int i = 0; i < ticks; i++) {
            if (--countdown < 0) { showObjects = !showObjects; countdown = 50; }
        }
    }
}
