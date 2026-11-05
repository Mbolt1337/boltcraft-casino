package com.mbolt.casino.game;

import com.mbolt.casino.game.roulette.RouletteLogic;
import com.mbolt.casino.game.roulette.RouletteWheel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteWheelTest {
    private static final int SPIN = 160;

    private static double angleBetween(double a, double b) {
        return Math.IEEEremainder(a - b, 360);
    }

    @Test
    void theBallComesToRestInTheChosenPocketForEveryNumber() {
        for (int number = 0; number <= 36; number++) {
            var pose = RouletteWheel.pose(number, SPIN, SPIN);
            double relative = pose.ballDegrees() - pose.wheelDegrees();
            assertEquals(0, angleBetween(relative, RouletteWheel.pocketAngle(number)), 1e-9, "pocket " + number);
            assertEquals(RouletteWheel.pocketRadius(number), pose.ballRadius(), 1e-9);
            assertEquals(RouletteWheel.POCKET_BOTTOM, pose.ballBottom(), 1e-9);
        }
    }

    @Test
    void theStartGivesNothingAway() {
        for (int number = 0; number <= 36; number++) {
            var pose = RouletteWheel.pose(number, 0, SPIN);
            assertEquals(0, pose.wheelDegrees(), 1e-9);
            assertEquals(0, angleBetween(pose.ballDegrees(), RouletteWheel.LAUNCH_ANGLE), 1e-9, "number " + number);
            assertEquals(RouletteWheel.TRACK_RADIUS, pose.ballRadius(), 1e-9);
        }
    }

    @Test
    void theBallRunsAgainstTheWheelThenRidesWithIt() {
        var before = RouletteWheel.pose(17, 10, SPIN);
        var after = RouletteWheel.pose(17, 11, SPIN);
        double ballEarly = after.ballDegrees() - before.ballDegrees();
        double wheelEarly = after.wheelDegrees() - before.wheelDegrees();
        assertTrue(wheelEarly > 0);
        assertTrue(ballEarly < 0, "the ball is thrown against the wheel");
        var settled = RouletteWheel.pose(17, 150, SPIN);
        var later = RouletteWheel.pose(17, 155, SPIN);
        assertEquals(settled.ballDegrees() - settled.wheelDegrees(), later.ballDegrees() - later.wheelDegrees(), 1e-9,
                "a settled ball stays in its pocket");
        var beforeDrop = RouletteWheel.pose(17, 80, SPIN);
        var dropping = RouletteWheel.pose(17, 115, SPIN);
        assertEquals(RouletteWheel.TRACK_RADIUS, beforeDrop.ballRadius());
        assertTrue(dropping.ballBottom() < beforeDrop.ballBottom());
        assertEquals(RouletteWheel.pose(17, SPIN, SPIN), RouletteWheel.pose(17, 10000, SPIN));
    }

    @Test
    void everyPocketHasItsOwnPlace() {
        for (int a = 0; a < RouletteLogic.POCKETS; a++) {
            for (int b = a + 1; b < RouletteLogic.POCKETS; b++) {
                assertTrue(Math.abs(angleBetween(RouletteWheel.pocketAngle(a), RouletteWheel.pocketAngle(b))) > 4,
                        a + " and " + b);
            }
        }
    }

    @Test
    void invalidTimeOrPocketCannotProduceAnAnimation() {
        assertThrows(IllegalArgumentException.class, () -> RouletteWheel.pose(37, 10, SPIN));
        assertThrows(IllegalArgumentException.class, () -> RouletteWheel.pose(0, Double.NaN, SPIN));
        assertThrows(IllegalArgumentException.class, () -> RouletteWheel.pose(0, 10, 0));
    }
}
