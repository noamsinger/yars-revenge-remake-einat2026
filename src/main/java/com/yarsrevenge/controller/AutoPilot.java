package com.yarsrevenge.controller;

import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.*;

public class AutoPilot {

    private static final double SNAP = 18.0;

    // Threat activation radii
    private static final double TORPEDO_THREAT_DIST = 140.0;
    private static final double CANNON_Y_MARGIN     = 200.0;

    // Nibble-before-cannon
    private static final double NIBBLE_COMMIT_SECS = 3.0;

    // Cell selection Y-band around Quotile
    private static final double CELL_Y_BAND = 250.0;

    // Approach offset above/below cell center to avoid shot stream
    private static final double NIBBLE_Y_OFFSET = 20.0;

    // Swirl warning safe position
    private static final double NZ_CENTER_X =
        (GameConstants.NZ_LEFT + GameConstants.NZ_RIGHT) / 2.0;

    // Evasion: how far ahead to project candidate positions
    private static final double DODGE_STEP   = 350.0;
    // Number of directions to sample (360 / DODGE_DIRS degrees apart)
    private static final int    DODGE_DIRS   = 24;
    // Shield cell penalty radius
    private static final double SHIELD_BLOCK_RADIUS = 80.0;

    // Relative weights: how much safety vs goal proximity matter
    private static final double WEIGHT_SAFETY = 1.0;
    private static final double WEIGHT_GOAL   = 1.5;

    private boolean wantCannon    = false;
    private double  nibbleTimer   = 0.0;

    public boolean consumeWantBullet() { return false; }
    public boolean consumeWantCannon() { boolean v = wantCannon; wantCannon = false; return v; }

    public void tick(double dt, GameState state) {
        if (state.getPhase() != GameState.Phase.PLAYING) return;

        Player  player  = state.getPlayer();
        double  px      = player.getCenterX();
        double  py      = player.getCenterY();
        boolean inNZ    = player.isInNeutralZone();
        Quotile quotile = state.getQuotile();

        Torpedo      torpedo = state.getTorpedo();
        Swirl        swirl   = state.getSwirl();
        ZorlonCannon cannon  = state.getZorlonCannon();

        // ── Priority 1: threat evasion ────────────────────────────────────────
        boolean torpedoThreat = torpedo != null && torpedo.isAlive()
            && Math.hypot(torpedo.getCenterX() - px, torpedo.getCenterY() - py) < TORPEDO_THREAT_DIST;

        boolean swirlWarning = quotile.getMode() == Quotile.Mode.MISSILE_WARNING;

        boolean swirlFlight = !swirlWarning
            && swirl != null && swirl.isAlive() && !swirl.isStalled();

        boolean cannonThreat = cannon != null && cannon.isAlive()
            && Math.abs(py - cannon.getCenterY()) < CANNON_Y_MARGIN;

        boolean threatened = torpedoThreat || swirlWarning || swirlFlight || cannonThreat;

        if (threatened) {
            if (swirlWarning) {
                moveToAlways(player, px, py, NZ_CENTER_X, 80.0);
            } else {
                double goalX = goalX(state, px, py);
                double goalY = goalY(state, px, py);
                double[] best = bestEvasionDir(px, py,
                    torpedoThreat ? torpedo : null,
                    swirlFlight   ? swirl   : null,
                    cannonThreat  ? cannon  : null,
                    state, goalX, goalY);
                moveToAlways(player, px, py, best[0], best[1]);
            }
            nibbleTimer = 0.0;
            return;
        }

        // ── Priority 2: fire cannon (after nibbling enough) ───────────────────
        GameConfig.GameMode mode = GameConfig.getInstance().getGameMode();

        if (mode == GameConfig.GameMode.ULTIMATE) {
            ultimateStrategy(player, px, py, inNZ, state, cannon, dt);
            return;
        }

        boolean cannonCharged = state.isCannonCharged();

        if (cannonCharged && cannon == null && nibbleTimer >= NIBBLE_COMMIT_SECS) {
            aimAndFireCannon(player, px, py, inNZ, quotile);
            return;
        }

        // ── Priority 3: nibble shield cells ───────────────────────────────────
        ShieldCell target = bestNibbleCell(state, px, py, quotile.getCenterY());
        if (target != null) {
            nibbleTimer += dt;
            double targetX = target.x - GameConstants.PLAYER_W * 0.25;
            double cellCY  = target.y + target.cellHeight / 2.0;
            double yOffset = (cellCY < quotile.getCenterY()) ? -NIBBLE_Y_OFFSET : NIBBLE_Y_OFFSET;
            moveToAlways(player, px, py, targetX, cellCY + yOffset);
            return;
        }

        nibbleTimer = 0.0;

        // ── Priority 4: touch Quotile to charge ───────────────────────────────
        if (!cannonCharged) {
            moveToAlways(player, px, py, quotile.getX() - 10, quotile.getCenterY());
            return;
        }

        // ── Priority 5: charged, no cannon yet → fire ─────────────────────────
        if (cannon == null) {
            aimAndFireCannon(player, px, py, inNZ, quotile);
            return;
        }

        // ── Priority 6: cannon in flight → keep nibbling / patrol ─────────────
        ShieldCell flying = bestNibbleCell(state, px, py, quotile.getCenterY());
        if (flying != null) {
            nibbleTimer += dt;
            double targetX = flying.x - GameConstants.PLAYER_W * 0.25;
            double cellCY  = flying.y + flying.cellHeight / 2.0;
            double yOffset = (cellCY < quotile.getCenterY()) ? -NIBBLE_Y_OFFSET : NIBBLE_Y_OFFSET;
            moveToAlways(player, px, py, targetX, cellCY + yOffset);
        } else {
            moveToAlways(player, px, py, GameConstants.NZ_LEFT - 200, GameConstants.LOGICAL_H / 2.0);
        }
    }

    // ── Ultimate mode ─────────────────────────────────────────────────────────

    private void ultimateStrategy(Player player, double px, double py,
                                   boolean inNZ, GameState state, ZorlonCannon cannon, double dt) {
        Quotile quotile = state.getQuotile();
        if (!state.hasTronsForCannon()) {
            ShieldCell target = bestNibbleCell(state, px, py, quotile.getCenterY());
            if (target != null) {
                nibbleTimer += dt;
                double cellCY  = target.y + target.cellHeight / 2.0;
                double yOffset = (cellCY < quotile.getCenterY()) ? -NIBBLE_Y_OFFSET : NIBBLE_Y_OFFSET;
                moveToAlways(player, px, py,
                    target.x - GameConstants.PLAYER_W * 0.25, cellCY + yOffset);
            } else {
                nibbleTimer = 0.0;
                moveToAlways(player, px, py, quotile.getX() - 10, quotile.getCenterY());
            }
        } else {
            double targetX = GameConstants.ULTIMATE_FIRE_X - 20;
            double travelSecs = (GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0 - targetX)
                / GameConstants.CANNON_SPEED;
            double interceptY = predictQuotileY(quotile, travelSecs);
            moveToAlways(player, px, py, targetX, interceptY);
            if (px <= GameConstants.ULTIMATE_FIRE_X + SNAP && cannon == null) {
                wantCannon = true;
            }
        }
    }

    // ── Evasion ───────────────────────────────────────────────────────────────

    /**
     * Sample DODGE_DIRS evenly-spaced directions. For each candidate position:
     *
     *   safety  = sum of (predicted separation from each threat at arrival time)
     *   goal    = closeness to the current objective
     *   score   = WEIGHT_SAFETY * safety + WEIGHT_GOAL * goal - shield_penalty
     *
     * Torpedo danger uses PREDICTED torpedo position when the player arrives there
     * (travel time = DODGE_STEP / PLAYER_SPEED), so moving toward it scores
     * naturally low because the torpedo will be right there on arrival.
     *
     * If every candidate scores identically (degenerate), pick a random direction.
     */
    private double[] bestEvasionDir(double px, double py,
                                     Torpedo torpedo, Swirl swirl, ZorlonCannon cannon,
                                     GameState state, double goalX, double goalY) {
        double stepSecs = DODGE_STEP / GameConstants.PLAYER_SPEED;

        // Predict torpedo position at arrival time
        double torpFutureX = 0, torpFutureY = 0;
        if (torpedo != null) {
            // Torpedo homes on current player position — in stepSecs it moves toward (px,py)
            double tdx  = px - torpedo.getCenterX();
            double tdy  = py - torpedo.getCenterY();
            double td   = Math.hypot(tdx, tdy);
            double tspd = torpedo.getWidth(); // speed stored as field — use via update logic
            // torpedo speed is stored internally; approximate: it closes ~half the gap in stepSecs
            // at TORPEDO_THREAT_DIST / stepSecs px/s. Use the raw Torpedo speed accessor.
            double torpSpeed = getTorpedoSpeed(torpedo);
            if (td > 1.0) {
                torpFutureX = torpedo.getCenterX() + (tdx / td) * torpSpeed * stepSecs;
                torpFutureY = torpedo.getCenterY() + (tdy / td) * torpSpeed * stepSecs;
            } else {
                torpFutureX = torpedo.getCenterX();
                torpFutureY = torpedo.getCenterY();
            }
        }

        // Swirl trajectory data
        double swirlDirX = 0, swirlDirY = 0, swirlOX = 0, swirlOY = 0;
        if (swirl != null) {
            double a = swirl.getAngle();
            swirlDirX = Math.cos(a); swirlDirY = Math.sin(a);
            swirlOX = swirl.getCenterX(); swirlOY = swirl.getCenterY();
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        double bestX = px + 1, bestY = py;
        boolean allEqual = true;
        double firstScore = Double.NaN;

        for (int i = 0; i < DODGE_DIRS; i++) {
            double angle = i * (2.0 * Math.PI / DODGE_DIRS);
            double cx = px + Math.cos(angle) * DODGE_STEP;
            double cy = py + Math.sin(angle) * DODGE_STEP;
            // Clamp X to playfield; Y unclamped (edge-hugging is valid)
            cx = Math.max(60, Math.min(GameConstants.NZ_RIGHT - 60, cx));

            double safety = 0;

            // Torpedo: distance from predicted future torpedo position
            if (torpedo != null) {
                safety += Math.hypot(cx - torpFutureX, cy - torpFutureY);
            }

            // Cannon beam: Y separation from beam
            if (cannon != null) {
                safety += Math.abs(cy - cannon.getCenterY());
            }

            // Swirl: perpendicular distance from trajectory line
            if (swirl != null) {
                double ex = cx - swirlOX;
                double ey = cy - swirlOY;
                safety += Math.abs(ex * swirlDirY - ey * swirlDirX);
            }

            // Goal: negative distance (closer = better)
            double goalPull = -Math.hypot(cx - goalX, cy - goalY);

            // Shield cell penalty
            double shieldPenalty = shieldCellsNear(state, cx, cy, SHIELD_BLOCK_RADIUS) * 200.0;

            double score = WEIGHT_SAFETY * safety + WEIGHT_GOAL * goalPull - shieldPenalty;

            if (Double.isNaN(firstScore)) firstScore = score;
            if (Math.abs(score - firstScore) > 1.0) allEqual = false;

            if (score > bestScore) {
                bestScore = score;
                bestX = cx;
                bestY = cy;
            }
        }

        // Degenerate: all directions score the same — pick a random one
        if (allEqual) {
            double angle = Math.random() * 2.0 * Math.PI;
            bestX = Math.max(60, Math.min(GameConstants.NZ_RIGHT - 60,
                             px + Math.cos(angle) * DODGE_STEP));
            bestY = py + Math.sin(angle) * DODGE_STEP;
        }

        return new double[]{ bestX, bestY };
    }

    /**
     * Extract torpedo speed. Torpedo stores speed = angularSpeed * 60 internally;
     * we read it via the entity's known update: it moves speed*dt per frame.
     * Since we can't access the private field directly, estimate from TORPEDO_THREAT_DIST:
     * typical torpedo covers ~180px/s at wave 1 (angularSpeed≈3 → speed=180).
     * We use a conservative 200 px/s which is good enough for a 0.4s lookahead.
     */
    private double getTorpedoSpeed(Torpedo torpedo) {
        // Torpedo speed = angularSpeed * 60; angularSpeed comes from WaveFactory.
        // We approximate with a fixed value; the prediction is direction-correct regardless.
        return 220.0;
    }

    // ── Cannon aim ────────────────────────────────────────────────────────────

    /**
     * Iteratively solve for intercept Y: fold in player Y-travel time before firing
     * so that when the cannon actually leaves, it is aimed at where Quotile will be.
     */
    private void aimAndFireCannon(Player player, double px, double py,
                                   boolean inNZ, Quotile quotile) {
        double quotileTargetX = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double interceptY = py;
        for (int iter = 0; iter < 4; iter++) {
            double playerMoveSecs  = Math.abs(interceptY - py) / GameConstants.PLAYER_SPEED;
            double cannonTravelSecs = (quotileTargetX - px) / GameConstants.CANNON_SPEED;
            interceptY = predictQuotileY(quotile, playerMoveSecs + cannonTravelSecs);
        }
        moveToAlways(player, px, py, px, interceptY);
        if (!inNZ && Math.abs(py - interceptY) < SNAP) {
            wantCannon = true;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double predictQuotileY(Quotile quotile, double travelSecs) {
        double minY  = 80.0;
        double maxY  = GameConstants.LOGICAL_H - 80.0;
        double range = maxY - minY;
        double proj  = quotile.getCenterY() + quotile.getSpeed() * quotile.getDirection() * travelSecs;
        proj -= minY;
        if (range > 0) {
            proj = proj % (2 * range);
            if (proj < 0) proj += 2 * range;
            if (proj > range) proj = 2 * range - proj;
        }
        return proj + minY;
    }

    private ShieldCell bestNibbleCell(GameState state, double px, double py, double quotileY) {
        ShieldCell bandBest = null; double bandDist = Double.MAX_VALUE;
        ShieldCell globBest = null; double globDist = Double.MAX_VALUE;
        for (ShieldCell cell : state.getShield().getCells()) {
            if (!cell.alive) continue;
            double cx  = cell.x + cell.cellWidth  / 2.0;
            double cy  = cell.y + cell.cellHeight / 2.0;
            double dFly = Math.hypot(cx - px, cy - py);
            if (dFly < globDist) { globDist = dFly; globBest = cell; }
            if (Math.abs(cy - quotileY) <= CELL_Y_BAND && dFly < bandDist) {
                bandDist = dFly; bandBest = cell;
            }
        }
        return bandBest != null ? bandBest : globBest;
    }

    private double goalX(GameState state, double px, double py) {
        ShieldCell c = bestNibbleCell(state, px, py, state.getQuotile().getCenterY());
        return c != null ? c.x - GameConstants.PLAYER_W * 0.25 : state.getQuotile().getX() - 10;
    }

    private double goalY(GameState state, double px, double py) {
        ShieldCell c = bestNibbleCell(state, px, py, state.getQuotile().getCenterY());
        return c != null ? c.y + c.cellHeight / 2.0 : state.getQuotile().getCenterY();
    }

    private void moveToAlways(Player player, double px, double py, double tx, double ty) {
        double dx = tx - px, dy = ty - py;
        double dist = Math.hypot(dx, dy);
        double spd  = GameConstants.PLAYER_SPEED;
        if (dist < 1.0) { player.setKeyboardVelocity(spd * 0.1, 0); return; }
        player.setKeyboardVelocity((dx / dist) * spd, (dy / dist) * spd);
    }

    private int shieldCellsNear(GameState state, double x, double y, double radius) {
        int count = 0;
        for (ShieldCell cell : state.getShield().getCells()) {
            if (!cell.alive) continue;
            double cx = cell.x + cell.cellWidth  / 2.0;
            double cy = cell.y + cell.cellHeight / 2.0;
            if (Math.hypot(cx - x, cy - y) < radius) count++;
        }
        return count;
    }
}
