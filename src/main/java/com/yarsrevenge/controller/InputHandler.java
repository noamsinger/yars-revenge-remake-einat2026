package com.yarsrevenge.controller;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.EnumSet;
import java.util.Set;

public class InputHandler {

    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private volatile boolean bulletFirePending      = false;
    private volatile boolean cannonFirePending      = false;
    private volatile boolean escapePending          = false;
    private volatile boolean debugTogglePending     = false;
    private volatile boolean autoPilotTogglePending = false;
    private volatile boolean debugKillEnemyPending  = false;
    private volatile boolean debugAddExpPending     = false;

    // Keep references so we can remove the exact same handler objects
    private EventHandler<KeyEvent> pressHandler;
    private EventHandler<KeyEvent> releaseHandler;

    public void attach(Scene scene) {
        pressHandler = e -> {
            pressedKeys.add(e.getCode());
            if (e.getCode() == KeyCode.ENTER)  bulletFirePending = true;
            if (e.getCode() == KeyCode.SPACE)  cannonFirePending = true;
            if (e.getCode() == KeyCode.ESCAPE) escapePending     = true;
            if (e.getCode() == KeyCode.D && pressedKeys.contains(KeyCode.SHIFT)) {
                debugTogglePending = true;
            }
            if (e.getCode() == KeyCode.A && pressedKeys.contains(KeyCode.SHIFT)) {
                autoPilotTogglePending = true;
            }
            if (e.getCode() == KeyCode.W) debugKillEnemyPending = true;
            if (e.getCode() == KeyCode.X) debugAddExpPending    = true;
        };
        releaseHandler = e -> pressedKeys.remove(e.getCode());

        // Use addEventHandler (not setOnKeyPressed) so we don't overwrite the
        // app-level addEventFilter that handles the M minimize key.
        scene.addEventHandler(KeyEvent.KEY_PRESSED,  pressHandler);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, releaseHandler);
    }

    public void detach(Scene scene) {
        if (pressHandler   != null) scene.removeEventHandler(KeyEvent.KEY_PRESSED,  pressHandler);
        if (releaseHandler != null) scene.removeEventHandler(KeyEvent.KEY_RELEASED, releaseHandler);
        pressHandler   = null;
        releaseHandler = null;
        pressedKeys.clear();
        bulletFirePending      = false;
        cannonFirePending      = false;
        escapePending          = false;
        debugTogglePending     = false;
        autoPilotTogglePending = false;
        debugKillEnemyPending  = false;
        debugAddExpPending     = false;
    }

    public boolean isKeyDown(KeyCode code) { return pressedKeys.contains(code); }

    public void clearKeys() { pressedKeys.clear(); }

    public boolean consumeBulletFire() {
        boolean v = bulletFirePending;
        bulletFirePending = false;
        return v;
    }

    public boolean consumeCannonFire() {
        boolean v = cannonFirePending;
        cannonFirePending = false;
        return v;
    }

    public boolean consumeEscape() {
        boolean v = escapePending;
        escapePending = false;
        return v;
    }

    public boolean consumeDebugToggle() {
        boolean v = debugTogglePending;
        debugTogglePending = false;
        return v;
    }

    public boolean consumeAutoPilotToggle() {
        boolean v = autoPilotTogglePending;
        autoPilotTogglePending = false;
        return v;
    }

    public boolean consumeDebugKillEnemy() {
        boolean v = debugKillEnemyPending;
        debugKillEnemyPending = false;
        return v;
    }

    public boolean consumeDebugAddExp() {
        boolean v = debugAddExpPending;
        debugAddExpPending = false;
        return v;
    }
}
