package bastion;

import robocode.*;
import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * Bastion - a robot by (Nicole & Fofolet)
 */
public class Bastion extends Robot {
	

	/**
	 * run: Bastion's default behavior
	 */
	public void run() {
		// Initialization of the robot should be put here

		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		setColors(Color.magenta, Color.cyan, Color.white, Color.red, Color.magenta);
		// body,gun,radar

		// Robot main loop
		while (true) {
			// teste de movimentação zigzag
			ahead(100);
			turnRight(120);
			turnGunRight(180);
			back(100);
			turnLeft(120);
			turnGunLeft(180);

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// teste de dano
		fire(3);
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// teste de recuo
		back(10);
	}

	public void onHitRobot(HitRobotEvent e) {
		if (e.isMyFault())
			;
		{
			turnRight(e.getBearing()); // retornar o ângulo do bastion em relação ao inimigo
			fire(5);
		}
	}

	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// teste de nível de burrice
		turnLeft(90);
	}

}
