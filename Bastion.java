package bastion;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.*;

/*
  - manter distancia segura
  - movimento lateral horizontal continuo
  - radar travado no alvo
  - mira preditiva iterativa com protecoes
  - tiros medios, apenas quando ha chance razoavel
  - evitar paredes e nao travar
  - priorizar inimigo parado ou com menos energia
*/

public class Bastion extends AdvancedRobot {

    // CONFIGURACAO
    private static final double WALL_MARGIN = 72;
    private static final double SAFE_MIN = 550;
    private static final double SAFE_MAX = 750;
    private static final double LONG_MOVE = 800; 
    private static final double MAX_VEL = 8;
    private static final double MAX_TURN_PER_TICK = Math.toRadians(35);
    private static final int HISTORY_LIMIT = 25;
    private static final int STALE_TICKS = 120;

    // ESTADO
    private final Map<String, Enemy> enemies = new HashMap<>();
    private String target = null;
    private double lateral = 1.0; 
    private final Random rnd = new Random();
    private long lastScanTime = 0;

    static class Enemy {
        String name;
        double x, y;
        double distance;
        double energy;
        double heading; 
        double velocity;
        long time;       
        Deque<Double> velHist = new ArrayDeque<>();
        Deque<Double> headHist = new ArrayDeque<>();
    }

    public void run() {
        setColors(Color.magenta, Color.cyan, Color.white, Color.red, Color.magenta);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setMaxVelocity(MAX_VEL);

        while (true) {
            cleanupOldEnemies();
            chooseTargetIfNeeded();

            if (target != null && enemies.containsKey(target)) {
                Enemy e = enemies.get(target);

                double abs = enemyAbsAngle(e);
                double radarTurn = Utils.normalRelativeAngle(abs - getRadarHeadingRadians());
                setTurnRadarRightRadians(radarTurn * 2.2);

                performMovement(e);
                performAimingAndFire(e);

                lastScanTime = getTime();
            } else {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                if (rnd.nextDouble() < 0.02) lateral *= -1;
                setAhead(LONG_MOVE * 0.5);
                setTurnRightRadians(Math.toRadians(20) * lateral);
            }
            execute();
        }
    }

    private void cleanupOldEnemies() {
        long now = getTime();
        enemies.values().removeIf(en -> (now - en.time) > STALE_TICKS);
        if (target != null && !enemies.containsKey(target)) target = null;
    }

    private void chooseTargetIfNeeded() {
        if (target != null && enemies.containsKey(target)) return;
        if (enemies.isEmpty()) {
            target = null;
            return;
        }
        String best = null;
        double bestEnergy = Double.MAX_VALUE;
        for (Enemy en : enemies.values()) {
            if (Math.abs(en.velocity) < 0.3) {
                best = en.name;
                break;
            }
            if (en.energy < bestEnergy) {
                bestEnergy = en.energy;
                best = en.name;
            }
        }
        target = best;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double absAngle = getHeadingRadians() + e.getBearingRadians();
        double ex = getX() + Math.sin(absAngle) * e.getDistance();
        double ey = getY() + Math.cos(absAngle) * e.getDistance();

        Enemy en = enemies.getOrDefault(e.getName(), new Enemy());
        en.name = e.getName();
        en.x = ex;
        en.y = ey;
        en.distance = e.getDistance();
        en.energy = e.getEnergy();
        en.heading = e.getHeadingRadians();
        en.velocity = e.getVelocity();
        en.time = getTime();

        if (en.velHist.size() >= HISTORY_LIMIT) en.velHist.removeFirst();
        if (en.headHist.size() >= HISTORY_LIMIT) en.headHist.removeFirst();
        en.velHist.addLast(en.velocity);
        en.headHist.addLast(en.heading);

        enemies.put(e.getName(), en);

        if (target == null) target = e.getName();
        else {
            Enemy cur = enemies.get(target);
            if (cur == null) target = e.getName();
            else {
                if (Math.abs(e.getVelocity()) < 0.3) target = e.getName();
                else if (e.getEnergy() < cur.energy - 1.0) target = e.getName();
            }
        }
    }

    // MOVIMENTO LATERAL CONTINUO
    private void performMovement(Enemy t) {
        double abs = enemyAbsAngle(t);
        double dist = t.distance;
        double desiredDist = SAFE_MIN + (SAFE_MAX - SAFE_MIN) * 0.5;

        if (lateral == 0) lateral = 1.0;

        double lateralAngle = abs + Math.PI / 2 * lateral;

        if (dist < SAFE_MIN + 40) {
            lateral *= -1;
            lateralAngle = abs + Math.PI / 2 * lateral;
        } else if (dist > SAFE_MAX - 40) {
            lateralAngle = abs + Math.PI / 2 * lateral;
        }

        lateralAngle += (rnd.nextDouble() - 0.5) * 0.08;

        Point2D.Double dest = pointAround(t.x, t.y, lateralAngle, desiredDist);

        if (destNearWall(dest.x, dest.y)) {
            lateral *= -1;
            lateralAngle = abs + Math.PI / 2 * lateral;
            dest = pointAround(t.x, t.y, lateralAngle, desiredDist);
        }

        dest = clampPointToField(dest);
        moveSmoothTo(dest.x, dest.y);
    }

    private void moveSmoothTo(double tx, double ty) {
        double angleToTarget = Math.atan2(tx - getX(), ty - getY());
        double turn = Utils.normalRelativeAngle(angleToTarget - getHeadingRadians());

        double moveDir = 1.0;
        if (Math.abs(turn) > Math.PI / 2) {
            turn = Utils.normalRelativeAngle(turn + Math.PI);
            moveDir = -1.0;
        }

        if (turn > MAX_TURN_PER_TICK) turn = MAX_TURN_PER_TICK;
        if (turn < -MAX_TURN_PER_TICK) turn = -MAX_TURN_PER_TICK;

        setTurnRightRadians(turn);
        setMaxVelocity(MAX_VEL);
        setAhead(LONG_MOVE * moveDir);
    }

    private void performAimingAndFire(Enemy e) {
        double power = 1.9;
        if (e.distance < 380) power = 2.4;
        if (e.energy < 12 && e.distance < 400) power = 2.6;
        if (getEnergy() < 18) power = Math.min(power, 1.6);
        power = Math.max(0.1, Math.min(3.0, power));

        double bulletSpeed = Rules.getBulletSpeed(power);
        double avgVel = averageDeque(e.velHist, e.velocity);
        double avgHead = averageDeque(e.headHist, e.heading);
        double accel = estimateAcceleration(e.velHist);
        double headVar = headingVariation(e.headHist);
        double curvature = Math.max(0.0, Math.min(0.04, 0.012 + headVar * 0.5));

        Point2D.Double pred = new Point2D.Double(e.x, e.y);
        double simHead = avgHead;
        double simVel = avgVel;
        int ticks = 0;

        while (ticks < 160) {
            double distToMe = Point2D.distance(pred.x, pred.y, getX(), getY());
            if (ticks * bulletSpeed >= distToMe) break;
            simVel += accel * 0.16;
            simHead += curvature * (rnd.nextDouble() - 0.5);
            pred.x += Math.sin(simHead) * simVel;
            pred.y += Math.cos(simHead) * simVel;
            pred.x = clamp(pred.x, WALL_MARGIN, getBattleFieldWidth() - WALL_MARGIN);
            pred.y = clamp(pred.y, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
            ticks++;
        }

        double aim = Math.atan2(pred.x - getX(), pred.y - getY());
        double gunTurn = Utils.normalRelativeAngle(aim - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);

        if (getGunHeat() == 0) {
            double alignTol = Math.toRadians(14);
            if (Math.abs(gunTurn) < alignTol) {
                double firePower = power;
                if (nearWallHere()) firePower = Math.min(firePower, 1.4);
                if (getEnergy() < 8) firePower = Math.min(firePower, 1.0);
                setFire(Math.max(0.1, Math.min(3.0, firePower)));
            }
        }
    }

    // UTILITARIOS 
    private double enemyAbsAngle(Enemy e) {
        return Math.atan2(e.x - getX(), e.y - getY());
    }

    private Point2D.Double pointAround(double x, double y, double ang, double dist) {
        return new Point2D.Double(x + Math.sin(ang) * dist, y + Math.cos(ang) * dist);
    }

    private Point2D.Double clampPointToField(Point2D.Double p) {
        p.x = clamp(p.x, WALL_MARGIN, getBattleFieldWidth() - WALL_MARGIN);
        p.y = clamp(p.y, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
        return p;
    }

    private boolean destNearWall(double x, double y) {
        return (x < WALL_MARGIN + 20 || x > getBattleFieldWidth() - (WALL_MARGIN + 20)
                || y < WALL_MARGIN + 20 || y > getBattleFieldHeight() - (WALL_MARGIN + 20));
    }

    private boolean nearWallHere() {
        double x = getX(), y = getY();
        return (x < WALL_MARGIN || x > getBattleFieldWidth() - WALL_MARGIN
                || y < WALL_MARGIN || y > getBattleFieldHeight() - WALL_MARGIN);
    }

    private double averageDeque(Deque<Double> d, double fallback) {
        if (d == null || d.isEmpty()) return fallback;
        double s = 0;
        for (double v : d) s += v;
        return s / d.size();
    }

    private double estimateAcceleration(Deque<Double> d) {
        if (d == null || d.size() < 2) return 0;
        double first = d.peekFirst();
        double last = d.peekLast();
        return (last - first) / d.size();
    }

    private double headingVariation(Deque<Double> d) {
        if (d == null || d.size() < 2) return 0.01;
        double mean = averageDeque(d, 0);
        double s = 0;
        for (double v : d) s += Math.abs(Utils.normalRelativeAngle(v - mean));
        return s / d.size();
    }

    private double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    public void onHitWall(HitWallEvent e) {
        lateral *= -1;
        setBack(100);
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        lateral *= -1;
        setAhead(80 * lateral);
        execute();
    }

    public void onHitRobot(HitRobotEvent e) {
        lateral *= -1;
        setBack(120);
        execute();
    }

    public void onRobotDeath(RobotDeathEvent e) {
        enemies.remove(e.getName());
        if (e.getName().equals(target)) target = null;
    }
}

