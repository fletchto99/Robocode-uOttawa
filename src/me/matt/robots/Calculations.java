package me.matt.robots;

import java.awt.Color;
import java.util.Random;

/**
 * This class is used for some calculations within our robot for uOttawa 2015 robo code.
 *
 * @author Matt Langlois (Fletchto99@gmail.com)
 * @author Yann Landry (yann.landry.94@gmail.com)
 * @author Joel Faubert
 */
public class Calculations {

    /**
     * Calculates a linear offset to aim our cannon (in degrees, because I hate radians)
     * 
     * @param velocity
     *            The velocity of the enemy tank.
     * @param heading
     *            The degrees the enemy tank is heading
     * @param bearing
     *            The bearing of the enemy tank, relative to our tank
     * @return The calculated degrees to offset our cannon by assuming the tank moves directly straight at the same velocity relative to our tank
     */
    public static double calculateLinearOffset(double velocity, double heading,
            double bearing) {
        return Math.toDegrees(Math.sin(Math.toRadians(heading - bearing)))
                * velocity;
    }

    /**
     * Get the power our cannon should fire at
     * 
     * @param distance
     *            The distance between our tank and the enemy tank
     * @param enemyVelocity
     *            The velocity of the enemy tank
     * @param minBulletPower
     *            The minimum velocity the cannon is allowed to fire at
     * @param maxBulletPower
     *            The maximum velocity the cannon is allowed to fire at
     * @return The power the cannon should fire at
     */
    public static double getPower(double distance, double enemyVelocity,
            double minBulletPower, double maxBulletPower) {
        /*
         * Local variable to keep track of the power
         */
        double power = maxBulletPower;

        /*
         * Only change the power if the enemy is moving
         */
        if (enemyVelocity > 0) {
            /*
             * Decrease the power the cannon fires at as the distance and velocity of the enemy increases
             */
            distance -= 150;
            enemyVelocity -= 4;
            while (distance > 0 && enemyVelocity > 0) {
                distance -= 150;
                enemyVelocity -= 4;
                power -= 0.5;
            }
        }

        /*
         * If our calculated power is lower than the minimum, fire at the minimum.
         */
        return Math.max(power, minBulletPower);
    }

    private static final Random random = new Random();

    /**
     * 
     * Generates a psudo-random value between the specified minimum and maximum values
     * 
     * @param min
     *            Minimum value to return (inclusive)
     * @param max
     *            Maximum value to return (inclusive)
     * @return A psudo-random double between the minimum and maximum value
     */
    public static double random(double min, double max) {
        return min + ((max - min) * random.nextDouble());
    }

    public static Color randomColour() {
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        return new Color(r, g, b);
    }

}
