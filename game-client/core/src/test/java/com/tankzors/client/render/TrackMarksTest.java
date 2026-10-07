package com.tankzors.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackMarksTest {

    @Test
    void stampsEverySpacingAlongMovementAxis() {
        TrackMarks marks = new TrackMarks();
        marks.beginFrame(0f);
        marks.track("p", 100, 100);
        marks.endFrame();
        assertEquals(0, marks.size());

        marks.beginFrame(0.016f);
        marks.track("p", 100, 100 + TrackMarks.STAMP_SPACING * 3);
        marks.endFrame();
        assertEquals(3, marks.size());
        assertFalse(marks.isHorizontal(0), "đi dọc → vạch xích nằm ngang");

        marks.beginFrame(0.016f);
        marks.track("p", 100 + TrackMarks.STAMP_SPACING, 100 + TrackMarks.STAMP_SPACING * 3);
        marks.endFrame();
        assertTrue(marks.isHorizontal(3));
    }

    @Test
    void ignoresStandingStillAndTeleports() {
        TrackMarks marks = new TrackMarks();
        marks.track("p", 100, 100);
        marks.track("p", 101, 100);
        marks.track("p", 600, 100);
        assertEquals(0, marks.size());
    }

    @Test
    void marksFadeOutAndExpire() {
        TrackMarks marks = new TrackMarks();
        marks.track("p", 0, 0);
        marks.track("p", TrackMarks.STAMP_SPACING, 0);
        assertEquals(1f, marks.opacity(0), 1e-6);
        marks.beginFrame(TrackMarks.LIFETIME_SECONDS / 2);
        assertEquals(0.5f, marks.opacity(0), 1e-3);
        marks.beginFrame(TrackMarks.LIFETIME_SECONDS);
        assertEquals(0, marks.size());
    }

    @Test
    void capacityIsBounded() {
        TrackMarks marks = new TrackMarks();
        marks.track("p", 0, 0);
        for (int i = 1; i <= TrackMarks.CAPACITY + 50; i++) {
            marks.track("p", 0, i * TrackMarks.STAMP_SPACING);
        }
        assertEquals(TrackMarks.CAPACITY, marks.size());
    }
}
