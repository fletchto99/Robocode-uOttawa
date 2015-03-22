package me.matt.robots;

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
    private final int SPACE_BETWEEN = 150;

    /**
     * The distance to travel when circling the enemy bot; before turning perpendicular again
     */
    private final int CIRCLE_TRAVEL_DISTANCE = 10;

    /**
     * The modifier which acts on the maximum velocity of the tank.
     */
    private final double MIN_VELOCITY = 2;

    /**
     * 75% of the time the velocity of our tank won't change
     */
    private final double VELOCITY_CHANGE = 2.5;

    /**
     * 40% of the time the tank will stop temporarly (to avoid bullets and confuse the enemy)
     */
    private final double STOP_CHANCE = 1.0;

    /*
     * Run the robot, setup some robot defaults
     */
    @Override
    public void run() {
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
        colorize();

        /*
         * Force the radar to constantly turn to the right, until an enemy is found then track them.
         */
        turnRadarRight(Integer.MAX_VALUE);

        /*
         * Start moving straight before we scan the battlefield, incase someone randomly scanned and fired at us first
         */
        setAhead(500);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {

        /*
         * The velocity at which our enemy istraveling
         */
        double enemyVelocity = e.getVelocity();

        /*
         * The distance from our bot to the enemy
         */
        double enemyDistance = e.getDistance();

        /*
         * The bearing of the enemy's tank
         */
        double enemyBearing = e.getBearing();

        /*
         * The Angle between our tank and the enemy relative to our direction
         */
        double driveAngle = getHeading() + enemyBearing;

        /*
         * The angle at which we need to aim our gun to have a direct lock on our enemy
         */
        double shootAngle = driveAngle - getGunHeading();

        /*
         * The offset which the guns should go to
         */
        double degreeOffset = Calculations.calculateLinearOffset(enemyVelocity,
                e.getHeading(), driveAngle);

        /*
         * Lock the radar onto the enemy
         */
        setTurnRadarLeft(getRadarTurnRemaining());

        /*
         * Change the color of our bot
         */
        colorize();

        /*
         * Determine the phase of our robot, targeting or circling
         */
        if (enemyDistance <= SPACE_BETWEEN) {
            /*
             * Circling phase -- Circle our enemy and fire at them, in hopes to avoid their bullets
             */
            circlingPhase(shootAngle, degreeOffset, enemyBearing);
        } else {
            /*
             * Move towards our enemies position and fire at them
             */
            targetingPhase(shootAngle, degreeOffset, enemyBearing);
        }

        /*
         * Move and fire at the enemy tanks
         */
        moveAndFire(enemyDistance, enemyVelocity);
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

    /**
     * Moves the tank and fires at the target with a few dodging techniques
     * 
     * @param enemyDistance
     *            The distance to the enemy
     * @param enemyVelocity
     *            The velocity of the enemies tank
     */
    private void moveAndFire(double enemyDistance, double enemyVelocity) {

        /*
         * Should we use the velocity change dodge technique
         */
        boolean changeVelocity = Calculations.random(0, 10) < VELOCITY_CHANGE;

        /*
         * Are we circling the enemy tank
         */
        boolean circling = enemyDistance < SPACE_BETWEEN;

        /*
         * Should we use the velocity change dodge technique
         */
        boolean stop = Calculations.random(0, 10) < STOP_CHANCE && !circling;

        /*
         * Should we change the velocity to throw off the enemy? Let's do this only 20% of the time. Too much will cause our tanks speed to change to
         * often making it hard to follow the enemy.
         */
        if (changeVelocity) {
            /*
             * Set the velocity to somewhere between min and max velocity (pixels per second)
             */
            setMaxVelocity(Calculations
                    .random(MIN_VELOCITY, Rules.MAX_VELOCITY));
        }

        /*
         * More dodging techniques
         */
        if (stop) {
            /*
             * Stop the tank temporarly
             */
            setAhead(0);
        } else {
            /*
             * Travel the distance between the two robots - the distance we wish to keep between the two bots
             * 
             * Otherwise travel the distance we wish to travel when circling the bot, when within circling range
             */
            setAhead(direction
                    * (circling ? enemyDistance - SPACE_BETWEEN
                            : CIRCLE_TRAVEL_DISTANCE));
        }

        /*
         * Set the tank to fire with a bullet power according to the chances of hitting enemy
         * The chances depend on how far away and how fast the enemy tank is traveling
         */
        setFire(Calculations.getPower(enemyDistance, enemyVelocity,
                Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER));
    }

    /**
     * Used to aim the gun while circling the target
     * 
     * @param shootAngle
     *            The direct angle to shoot at
     * @param degreeOffset
     *            The offset calculation
     * @param enemyBearing
     *            The bearing of the enemy's tank
     */
    private void circlingPhase(double shootAngle, double degreeOffset,
            double enemyBearing) {
        /*
         * Aim the gun at our enemy using the direct angle + an offset
         */
        setTurnGunRight(Utils.normalRelativeAngleDegrees(shootAngle
                + (degreeOffset / 11)));

        /*
         * Turn the tank perpindicular to the enemy and prepare to circle them
         */
        setTurnLeft(-1 * (90 + enemyBearing));
    }

    /**
     * Used to aim the gun while driving towards a target
     * 
     * @param shootAngle
     *            The direct angle to shoot at
     * @param degreeOffset
     *            The offset calculation
     * @param enemyBearing
     *            The bearing of the enemy's tank
     */
    private void targetingPhase(double shootAngle, double degreeOffset,
            double enemyBearing) {
        /*
         * Aim the gun at our enemy using the direct angle + a larger offset to compensate for distance and velocity
         */
        setTurnGunRight(Utils.normalRelativeAngleDegrees(shootAngle
                + (degreeOffset / 14.5)));

        /*
         * Travel towards our enemy based on their estimated position and our velocity, to compensate for their velocity
         */
        setTurnRight(Utils.normalRelativeAngleDegrees(enemyBearing
                + (degreeOffset / getVelocity())));
    }

    /**
     * Changes the tank to all random colours
     */
    private void colorize() {
        /*
         * Personlize each secion differently
         */
        setScanColor(Calculations.randomColour());
        setBulletColor(Calculations.randomColour());
        setColors(Calculations.randomColour(), Calculations.randomColour(),
                Calculations.randomColour());
    }

}