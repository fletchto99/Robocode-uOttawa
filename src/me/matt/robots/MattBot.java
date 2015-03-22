package me.matt.robots;

import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

/**
 * This is the main robot class for Robocode uOttawa 2015.
 * 
 * It employs a tactic that will drive directly towards the target until it gets a safe distance away from the robot. Once it is a safe distance away
 * it employs dodging techniques. It has 4 techniques:
 * 
 * 1. Stop and go - Occasionally the robot will stop for a split second
 * 2. Velocity change - Set the velocity to a psudo-random number between the MIN_VELOCITY and MAX_VELOCITY
 * 3. Oscelating flower pattern driving when near the enemy
 * 4. Multi colour camouflage - Dosn't really help dodge but it just looks funny
 * 
 * It has 1 gun strategy:
 * 
 * 1. Shoot the distance in front of the enemy according to their velocity and direction of travel
 *
 * Tips and tricks from: http://www.ibm.com/developerworks/java/library/j-robotips/index.html
 * 
 * and
 * 
 * http://www.ibm.com/developerworks/java/library/j-tipstrats/index.html
 * 
 * @author Matt Langlois (Fletchto99@gmail.com)
 */
public class MattBot extends AdvancedRobot {

    /**
     * An integer representing the direction of travel.
     * 
     * true - Forward
     * false - Reverse
     */
    private boolean forward = true;

    /**
     * The distance that we define as safe between our robot and the enemy, any closer and we might collide
     */
    private final int SAFE_DISTANCE = 130;

    /**
     * The distance to travel when dodging the enemy bot; before turning perpendicular again
     */
    private final int DODGE_TRAVEL_DISTANCE = 10;

    /**
     * The modifier which acts on the maximum velocity of the tank.
     */
    private final double MIN_VELOCITY = 6;

    /**
     * 75% of the time the velocity of our tank won't change
     */
    private final double VELOCITY_CHANGE = 20;

    /**
     * 40% of the time the tank will stop temporarily (to avoid bullets and confuse the enemy)
     */
    private final double STOP_CHANCE = 1.5;

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
         * The distance from our bot to the enemy
         */
        double enemyDistance = e.getDistance();

        /*
         * Are we closer than a safe distance to our enemy? If so deploy phase 2
         */
        boolean safe = enemyDistance <= SAFE_DISTANCE + DODGE_TRAVEL_DISTANCE;

        /*
         * The velocity at which our enemy istraveling
         */
        double enemyVelocity = e.getVelocity();

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
         * Change the color of our bot to random!
         */
        colorize();

        /*
         * Determine the phase of our robot, targeting or dodging
         */
        if (safe) {
            /*
             * Dodging phase -- Move around our enemy and fire at them, in hopes to avoid their bullets
             */
            dodgingPhase(shootAngle, degreeOffset, enemyBearing);
        } else {
            /*
             * Move towards our enemies position and fire at them
             */
            targetingPhase(shootAngle, degreeOffset, enemyBearing);
        }

        /*
         * Move and fire at the enemy tanks
         */
        moveAndFire(enemyDistance, enemyVelocity, safe);
    }

    /**
     * If we've hit a wall stop traveling in that direction
     */
    @Override
    public void onHitWall(HitWallEvent e) {
        /*
         * Travel in the opposite direction, so we're not just a sitting duck
         */
        forward = !forward;
    }

    /**
     * Moves the tank and fires at the target with a few dodging techniques
     * 
     * @param enemyDistance
     *            The distance to the enemy
     * @param enemyVelocity
     *            The velocity of the enemies tank
     */
    private void moveAndFire(double enemyDistance, double enemyVelocity,
            boolean safeDistance) {

        /*
         * Should we use the velocity change dodge technique
         */
        boolean changeVelocity = Calculations.random(0, 10) < VELOCITY_CHANGE;

        /*
         * Should we use the velocity change dodge technique
         */
        boolean stop = Calculations.random(0, 10) < STOP_CHANCE
                && !safeDistance;

        /*
         * 
         */
        double power = Calculations.getPower(enemyDistance, enemyVelocity,
                Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);

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
         * More dodging techniques Stopping changing velocity
         */
        if (stop) {
            /*
             * Stop the tank temporarily
             */
            setAhead(0);
        } else {
            /*
             * Travel the distance between the two robots - the distance we wish to keep between the two bots
             * 
             * Otherwise travel the distance we wish to travel when dodging the bot
             */
            setAhead((forward ? 1 : -1)
                    * (!safeDistance ? enemyDistance - SAFE_DISTANCE
                            : DODGE_TRAVEL_DISTANCE));
        }

        /*
         * Set the tank to fire with a bullet power according to the chances of hitting enemy
         * The chances depend on how far away and how fast the enemy tank is traveling
         */
        setFire(power);
    }

    /**
     * Used to aim the gun while dodging the target
     * 
     * @param shootAngle
     *            The direct angle to shoot at
     * @param degreeOffset
     *            The offset calculation
     * @param enemyBearing
     *            The bearing of the enemy's tank
     */
    private void dodgingPhase(double shootAngle, double degreeOffset,
            double enemyBearing) {

        /*
         * Aim the gun at our enemy using the direct angle + an offset
         */
        setTurnGunRight(Calculations.calculateNormalAngleToEnemy(shootAngle,
                degreeOffset, 11));

        /*
         * Turn the tank perpendicular to the enemy and prepare to dodge them in an oscelating flower pattern
         */
        setTurnRight((forward ? 1 : -1) * (enemyBearing - 90));
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
         * Aim the gun at our enemy using the direct Normal angle + a larger offset to compensate for distance and velocity
         */
        setTurnGunRight(Calculations.calculateNormalAngleToEnemy(shootAngle,
                degreeOffset, 13.5));

        /*
         * Travel towards our enemy based on their estimated position and our velocity, to compensate for their velocity
         */
        setTurnRight(Calculations.calculateNormalAngleToEnemy(enemyBearing,
                degreeOffset, getVelocity()));
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