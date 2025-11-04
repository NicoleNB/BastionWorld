package bastion;
import robocode.*;
import robocode.util.Utils;
import java.util.Random;

public class Bastion extends AdvancedRobot {

    // Informações do inimigo
    double energiaInimigo = 100;
    double anguloRelativoInimigo;
    double distanciaInimigo;
    double direcaoInimigo;
    double velocidadeInimigo;
    double anguloAbsolutoInimigo;
    double posXInimigo, posYInimigo;

    int direcaoMovimento = 1;
    Random aleatorio = new Random();
    double distanciaAlvo = 300; 
    public void run() {
        // Cores do robô
        setColors(java.awt.Color.DARK_GRAY, java.awt.Color.BLACK, java.awt.Color.RED);


        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        while (true) {
            execute(); // executa comandos simultaneamente
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Atualiza informações do inimigo
        anguloRelativoInimigo = e.getBearingRadians();
        distanciaInimigo = e.getDistance();
        direcaoInimigo = e.getHeadingRadians();
        velocidadeInimigo = e.getVelocity();
        anguloAbsolutoInimigo = getHeadingRadians() + anguloRelativoInimigo;

        posXInimigo = getX() + Math.sin(anguloAbsolutoInimigo) * distanciaInimigo;
        posYInimigo = getY() + Math.cos(anguloAbsolutoInimigo) * distanciaInimigo;

        // Radar travado no inimigo com overscan
        double giroRadar = Utils.normalRelativeAngle(anguloAbsolutoInimigo - getRadarHeadingRadians());
        setTurnRadarRightRadians(giroRadar * 2);

        // Se o inimigo disparou (queda de energia), realizar evasão forte
        if (e.getEnergy() < energiaInimigo && e.getEnergy() > 0) {
            esquivar(true);
        }
        energiaInimigo = e.getEnergy();

        // Movimento lateral contínuo para evitar ser atingido
        esquivar(false);

        // Mira preditiva e disparo
        double potenciaTiro = escolherPotenciaTiro();
        double anguloMira = calcularAnguloPreditivo(posXInimigo, posYInimigo, velocidadeInimigo, direcaoInimigo, potenciaTiro);

        setTurnGunRightRadians(Utils.normalRelativeAngle(anguloMira - getGunHeadingRadians()));
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < Math.toRadians(10)) {
            setFire(potenciaTiro);
        }

        scan(); // garante que o radar continue escaneando
    }

    // Escolhe potência do tiro baseada na distância e energia
    private double escolherPotenciaTiro() {
        double potencia;
        if (distanciaInimigo < 150) potencia = 3;
        else if (distanciaInimigo < 300) potencia = 2;
        else potencia = 1;

        if (getEnergy() < 15) {
            potencia = Math.min(potencia, 1);
        }

        if (energiaInimigo < 10 && distanciaInimigo < 250) {
            potencia = Math.max(potencia, 2);
        }

        return Math.min(potencia, getEnergy());
    }

    // Método de evasão lateral e imprevisível
    private void esquivar(boolean evasaoForte) {
        double distanciaDesejada = distanciaAlvo + (aleatorio.nextDouble() - 0.5) * 80;
        double frente = distanciaInimigo - distanciaDesejada;

        double perpendicular = anguloAbsolutoInimigo + Math.PI / 2;
        double anguloMovimento = perpendicular + (aleatorio.nextDouble() - 0.5) * 0.6;

        if (evasaoForte || aleatorio.nextDouble() < 0.08) {
            direcaoMovimento *= -1;
        }

        setTurnRightRadians(Utils.normalRelativeAngle(anguloMovimento - getHeadingRadians()));
        setAhead(frente * direcaoMovimento);
    }

    // Calcula ângulo preditivo linear do inimigo
    private double calcularAnguloPreditivo(double targetX, double targetY, double targetVel, double targetDir, double power) {
        double velocidadeBala = 20 - 3 * power;
        double myX = getX();
        double myY = getY();

        double predX = targetX;
        double predY = targetY;

        for (int i = 0; i < 80; i++) {
            double dx = predX - myX;
            double dy = predY - myY;
            double distancia = Math.hypot(dx, dy);
            double tempo = distancia / velocidadeBala;

            predX = targetX + Math.sin(targetDir) * targetVel * tempo;
            predY = targetY + Math.cos(targetDir) * targetVel * tempo;

            predX = Math.max(18, Math.min(getBattleFieldWidth() - 18, predX));
            predY = Math.max(18, Math.min(getBattleFieldHeight() - 18, predY));
        }

        return Math.atan2(predX - myX, predY - myY);
    }

    // Eventos de colisão e tiros

    public void onHitByBullet(HitByBulletEvent e) {
        direcaoMovimento *= -1;
        setAhead((50 + aleatorio.nextDouble() * 150) * direcaoMovimento);
    }

    public void onHitWall(HitWallEvent e) {
        direcaoMovimento *= -1;
        setBack(50);
        setTurnRight(90);
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()) {
            setBack(50);
            direcaoMovimento *= -1;
        } else {
            setAhead(40);
            setTurnGunRightRadians(Utils.normalRelativeAngle(anguloAbsolutoInimigo - getGunHeadingRadians()));
            if (getGunHeat() == 0) setFire(2);
        }
    }

    public void onWin(WinEvent e) {
        for (int i = 0; i < 10; i++) {
            turnRight(30);
            turnLeft(30);
        }
    }
}