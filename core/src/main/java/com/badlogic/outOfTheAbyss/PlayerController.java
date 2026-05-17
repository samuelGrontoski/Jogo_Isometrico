package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class PlayerController extends InputAdapter {
    public boolean up, down, left, right;
    public boolean shiftPressed;
    public boolean ctrlPressed;
    public boolean escapePressed;

    // Triggers (Eventos de disparo único)
    private boolean dashTriggered;
    private boolean attackTriggered;
    private boolean fullscreenTriggered;

    // Estados de Depuração (Estilo Minecraft F3)
    private boolean f3Held = false;
    private boolean f3ActionUsed = false;
    private boolean debugInfoToggle = false;
    private boolean hitboxesToggle = false;

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W: up = true; break;
            case Input.Keys.S: down = true; break;
            case Input.Keys.A: left = true; break;
            case Input.Keys.D: right = true; break;
            case Input.Keys.SHIFT_LEFT: shiftPressed = true; break;
            case Input.Keys.CONTROL_LEFT: ctrlPressed = true; break;
            case Input.Keys.ESCAPE: escapePressed = true; break;
            case Input.Keys.SPACE: dashTriggered = true; break;
            case Input.Keys.F11: fullscreenTriggered = true; break;

            // Lógica de atalhos de depuração
            case Input.Keys.F3:
                f3Held = true;
                f3ActionUsed = false; // Começamos assumindo que é apenas o F3
                break;

            case Input.Keys.B:
                if (f3Held) {
                    hitboxesToggle = true; // Dispara a troca das hitboxes
                    f3ActionUsed = true;   // Marca que o F3 foi usado para um combo
                }
                break;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W: up = false; break;
            case Input.Keys.S: down = false; break;
            case Input.Keys.A: left = false; break;
            case Input.Keys.D: right = false; break;
            case Input.Keys.SHIFT_LEFT: shiftPressed = false; break;
            case Input.Keys.CONTROL_LEFT: ctrlPressed = false; break;
            case Input.Keys.ESCAPE: escapePressed = false; break;

            case Input.Keys.F3:
                f3Held = false;
                if (!f3ActionUsed) {
                    debugInfoToggle = true;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            attackTriggered = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // --- Métodos de Consumo de Trigger ---
    public boolean consumeDash() {
        if (dashTriggered) {
            dashTriggered = false;
            return true;
        }
        return false;
    }

    public boolean consumeAttack() {
        if (attackTriggered) {
            attackTriggered = false;
            return true;
        }
        return false;
    }

    public boolean consumeFullscreenToggle() {
        if (fullscreenTriggered) {
            fullscreenTriggered = false;
            return true;
        }
        return false;
    }

    public boolean consumeDebugInfoToggle() {
        if (debugInfoToggle) {
            debugInfoToggle = false;
            return true;
        }
        return false;
    }

    public boolean consumeHitboxesToggle() {
        if (hitboxesToggle) {
            hitboxesToggle = false;
            return true;
        }
        return false;
    }
}
