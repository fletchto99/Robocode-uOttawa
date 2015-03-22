package me.matt.robots;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * This is the main robot class for Robocode uOttawa 2015.
 *
 * @author Matt Langlois (Fletchto99@gmail.com)
 * @author Yann Landry (yann.landry.94@gmail.com)
 * @author Joel Faubert
 */
public class MattBot extends AdvancedRobot {

    /**
     * An integer representing the direction of travel.
     * 
     * 1 - Forward
     * 0 - Stopped
     * -1 - Reverse
     */
    private int direction = 1;

    /**
     * The space that should be maintained between our bot and the enemy
     */
    private final int SPACE = 100;

    /**
     * The distance to travel when circling the enemy bot; before turning perpendicular again
     */
    private final int CIRCLE_TRAVEL_DISTANCE = 10;

    /*
     * Run the robot, setup some robot defaults
     */
    @Override
    public void run() {
        setup();
    }

    private void setup() {
        /*
         * Prevent the radar and gun from turning with the body of the robot
         * 
         * Allow the two to move freely
         */
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        /*
         * Let's personalize the robot
         */
        setScanColor(Color.RED);
        setBulletColor(Color.ORANGE);
        setColors(Color.GREEN, Color.BLACK, Color.WHITE);

        /*
         * Force the radar to constantly turn to the right, until an enemy is found then track them.
         */
        turnRadarRight(Double.MAX_VALUE);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {

        /*
         * The bearing of enemy robot, relative to our robot
         */
        double enemyBearing = getHeading() + e.getBearing();

        /*
         * The velocity at which our enemy istraveling
         */
        double enemyVelocity = e.getVelocity();

        /*
         * 
         */
        double degreeOffset = Calculations.calculateLinearOffset(enemyVelocity,
                e.getHeading(), enemyBearing);

        /*
         * Lock the radar onto the enemy
         */
        setTurnRadarLeft(getRadarTurnRemaining());

        if (e.getDistance() > SPACE) {
            setTurnGunRight(Utils.normalRelativeAngleDegrees(enemyBearing
                    - getGunHeading() + degreeOffset / 20));
            setTurnRight(Utils.normalRelativeAngleDegrees(enemyBearing
                    - getHeading() + degreeOffset));
        } else {
            setTurnGunRight(Utils.normalRelativeAngleDegrees(enemyBearing
                    - getGunHeading() + degreeOffset / 40));
            setTurnLeft(-1 * (90 + e.getBearing()));
        }
        moveAndFire(e.getDistance(), e.getVelocity(),
                Calculations.random(0, 10) > 8);
    }

    /**
     * If we've hit a wall stop traveling in that direction
     */
    @Override
    public void onHitWall(HitWallEvent e) {
        /*
         * Travel in the opposite direction, so we're not just a sitting duck
         */
        direction = -direction;
    }

    private void moveAndFire(double enemyDistance, double enemyVelocity,
            boolean changeVelocity) {
        /*
         * Should we change the velocity to throw off the enemy? Let's do this only 20% of the time. Too much will cause our tanks speed to change to
         * often making it hard to follow the enemy.
         */
        if (changeVelocity) {
            /*
             * Set the velocity to somewhere between max-8 and max velocity (pixels per second)
             * 
             * We use 7 because that is the default maximum velocity +1, hence the minimum would be 1 (so our robot always moves)
             */
            setMaxVelocity(Calculations.random(
                    Math.max(Rules.MAX_VELOCITY - 7, 0), Rules.MAX_VELOCITY));
        }

        /*
         * Travel the distance between the two robots - the distance we wish to keep between the two bots
         * 
         * Otherwise travel the distance we wish to travel when circling the bot, when within circling range
         */
        setAhead(direction
                * (enemyDistance > SPACE ? enemyDistance - SPACE
                        : CIRCLE_TRAVEL_DISTANCE));
        /*
         * Set the tank to fire with a bullet power according to the chances of hitting enemy
         * The chances depend on how far away and how fast the enemy tank is traveling
         */
        setFire(Calculations.getPower(enemyDistance, enemyVelocity,
                Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER));// Fire using a power according to the distance of the enemy
    }

}