package com.tankzors.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreCalculatorTest {

    @Test
    void exampleFromProjectDoc() {
        // Còn 75HP, hạ 2 AI, hết 45 giây -> 75*10 + 2*200 + (1000 - 450) = 1700
        assertEquals(1700, ScoreCalculator.calculate(75, 2, 45));
    }

    @Test
    void zeroTimeGivesMaxTimeBonus() {
        assertEquals(1000, ScoreCalculator.calculate(0, 0, 0));
    }

    @Test
    void longTimeClampsTimeBonusToZero() {
        assertEquals(150, ScoreCalculator.calculate(15, 0, 200));
    }

    @Test
    void negativeInputsClampedToZero() {
        // hp=-10 -> hp part=0, aiKills=-2 -> kill part=0, time=-5 -> timeBonus=1000 (clamp)
        assertEquals(1000, ScoreCalculator.calculate(-10, -2, -5));
    }

    @Test
    void estimateAiKillsFromScoreMultiple100() {
        assertEquals(2, ScoreCalculator.estimateAiKillsFromAccumulatedScore(200));
        assertEquals(0, ScoreCalculator.estimateAiKillsFromAccumulatedScore(50));
        assertEquals(3, ScoreCalculator.estimateAiKillsFromAccumulatedScore(300));
    }
}
