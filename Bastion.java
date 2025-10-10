package bastion;

import robocode.*;
import java.awt.Color;
import java.util.Random;
import java.awt.geom.Point2D


/**
 * Bastion - a robot by (Nicole & Fofolet)
 */
public class Bastion extends Robot {
	//informacoes do inimigo
	double enemyEnergy = 100; //energia do inimigo
	double enemyAngulo; //angulo da direcao que o inimigo esta
	double enemyDistancia; //distancia ate o inimigo
	double enemyMov; //angulo da direcao que o inimigo esta se movendo
	double enemyAngAbs; //angulo absoluto, meu angulo x angulo inimigo. posiciona o radar

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
		enemyAngAbs = e.getHeadingRadians

		//calcula a posicao do inimigo
		enemyX = getX + Math.sin(enemyAngAbs) * enemyDistancia;
		enemy = getY + Math.cos(enemyAngAbs) * enemyDistancia;

		//continua rastreando
		double radarGira = Utilis.normalRelativeAngle(enemyAngAbs - getRadarHeadingRadians());

		setTurnRadarRightRadian(Utilis.normalRelativeAngle(radarTurn)*2); //escaneia 2x para garantir

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