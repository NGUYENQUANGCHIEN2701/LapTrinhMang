package com.tankzors.client.gameplay.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Parity server application.ScoreCalculatorTest (bỏ hàm estimate đã không còn ở client). */
class ScoreCalculatorTest {

    @Test
    void exampleFromProjectDoc() {
        // 75HP, 2 kills, 45s -> 75*10 + 2*200 + (1000-450) = 1700
        assertEquals(1700, ScoreCalculator.calculate(75, 2, 45));
    }

    @Test
    void zeroTimeGivesMaxBonus() {
        assertEquals(1000, ScoreCalculator.calculate(0, 0, 0));
    }

    @Test
    void longTimeClampsToZero() {
        // 15HP*10=150 + 0 kills + time clamped 0 = 150
        assertEquals(150, ScoreCalculator.calculate(15, 0, 200));
    }

    @Test
    void negativeInputsClamped() {
        // hp=-10 -> 0, kills=-2 -> 0, time=-5 -> clamp 0, timeBonus=1000
        assertEquals(1000, ScoreCalculator.calculate(-10, -2, -5));
    }

    @Test
    void perfectRunMaxScore() {
        // 100HP, 3 kills, 0s = 1000 + 600 + 1000 = 2600
        assertEquals(2600, ScoreCalculator.calculate(100, 3, 0));
    }
}
