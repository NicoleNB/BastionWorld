package bastion;

import robocode.*;
import java.awt.Color;
import java.util.Random;

/**
 * Bastion - a robot by (Nicole & Fofolet)
 */
public class Bastion extends AdvancedRobot {
	//informacoes do inimigo
	double enemyEnergy = 100; //energia do inimigo
	double enemyAngulo; //angulo da direcao que o inimigo esta
	double enemyDistancia; //distancia ate o inimigo
	double enemyMov; //angulo da direcao que o inimigo esta se movendo
	double enemyX, enemyY; //angulo absoluto, meu angulo x angulo inimigo. posiciona o radar

	int move = 1;
	Random lerolero = new Random(); //gera movimento aleatorio
	double distanciaAlvo = 300; //teste de distancia do inimigo

	public void run() {
		setColors(Color.magenta, Color.cyan, Color.white, Color.red, Color.magenta);
		
		setAdjustGunForRobotTurn(true); //arma independente do corpinho
		setAdjustRadarForRobotTurn(true); //radar independente do corpinho
		setAdjustRadarForGunTurn(true); //radar independente do cano

		// inicialmente...
		while (true) {
			// teste de movimentacao, aqui ele so vai aguardar os eventos
			execute();

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// teste de dano
		enemyAngulo = e.getBearingRadians();
		enemyDistancia = e.getDistance();
		enemyMov = e.getVelocity();

		//queda de energia do inimigo? entao: !!!!!!!!CONTINUAR DAQUI!!!!!!!!!!!!
	}

	
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
