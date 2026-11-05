package com.mbolt.casino.game.roulette;

/**
 * Where the wheel and the ball are during a spin, in the table model's pixels. The server supplies the chosen
 * number; the wheel slows to a stop while the ball runs against it on the track, drops in and comes to rest in
 * that number's pocket, then turns with the wheel.
 */
public final class RouletteWheel {
    /** The ball rolls on the track at this radius from the wheel centre, its bottom at this height. */
    public static final double TRACK_RADIUS = 6.5;
    public static final double TRACK_BOTTOM = 14;
    /** Height of the ball's bottom once it lies in a pocket. */
    public static final double POCKET_BOTTOM = 13.25;
    /** The ball is launched from the croupier's side of the wheel, whatever the result. */
    public static final double LAUNCH_ANGLE = 180;
    private static final double WHEEL_TURNS = 3;
    /** Turns the ball makes against the wheel before it settles. */
    private static final double BALL_TURNS = 14;
    private static final double DROP_START = .6;
    private static final double DROP_END = .82;
    private static final double SETTLED = .9;

    /*
     * Each pocket's place on the wheel, in wheel order (RouletteLogic.WHEEL): its angle in degrees from the front
     * of the wheel, turning the way the bones turn, and its distance from the centre. They are the centres of the
     * pocket pixels drawn in the head tile of art/textures/roulette_table.txt; art/roulette_table_model.py prints
     * them whenever that drawing changes.
     */
    private static final double[] POCKET_ANGLES = {
            174.81, 164.74, 155.56, 150.95, 142.13, 127.87, 119.05, 114.44, 105.26, 95.19, 84.81, 74.74,
            65.56, 52.13, 37.87, 29.05, 24.44, 15.26, 5.19, 354.81, 344.74, 335.56, 322.13, 307.87, 299.05,
            294.44, 285.26, 275.19, 264.81, 254.74, 245.56, 240.95, 232.13, 217.87, 204.44, 195.26, 185.19};
    private static final double[] POCKET_RADII = {
            5.523, 5.701, 6.042, 5.148, 5.701, 5.701, 5.148, 6.042, 5.701, 5.523, 5.523, 5.701, 6.042, 5.701, 5.701,
            5.148, 6.042, 5.701, 5.523, 5.523, 5.701, 6.042, 5.701, 5.701, 5.148, 6.042, 5.701, 5.523, 5.523, 5.701,
            6.042, 5.148, 5.701, 5.701, 6.042, 5.701, 5.523};

    private RouletteWheel() {
    }

    /** Angle of the number's pocket relative to the wheel. */
    public static double pocketAngle(int number) {
        return POCKET_ANGLES[RouletteLogic.WHEEL.indexOf(number)];
    }

    public static double pocketRadius(int number) {
        return POCKET_RADII[RouletteLogic.WHEEL.indexOf(number)];
    }

    public static Pose pose(int number, double elapsedTicks, int durationTicks) {
        RouletteLogic.requireNumber(number);
        if (durationTicks <= 0 || !Double.isFinite(elapsedTicks)) {
            throw new IllegalArgumentException("Invalid wheel animation time");
        }
        double progress = Math.clamp(elapsedTicks / durationTicks, 0, 1);
        double wheel = WHEEL_TURNS * 360 * (1 - Math.pow(1 - progress, 3));
        // Ball angle relative to the wheel. It starts at the launch point and ends in the pocket; the turns in
        // between run against the wheel and slow down until the ball settles.
        double pocket = pocketAngle(number);
        double run = BALL_TURNS * 360 + ((LAUNCH_ANGLE - pocket) % 360 + 360) % 360;
        double settling = Math.clamp(progress / SETTLED, 0, 1);
        double relative = pocket + run * Math.pow(1 - settling, 2);
        double drop = Math.clamp((progress - DROP_START) / (DROP_END - DROP_START), 0, 1);
        drop = drop * drop * (3 - 2 * drop);
        double radius = TRACK_RADIUS + (pocketRadius(number) - TRACK_RADIUS) * drop;
        double bottom = TRACK_BOTTOM + (POCKET_BOTTOM - TRACK_BOTTOM) * drop;
        return new Pose(wheel, wheel + relative, radius, bottom);
    }

    public record Pose(double wheelDegrees, double ballDegrees, double ballRadius, double ballBottom) {
    }
}
