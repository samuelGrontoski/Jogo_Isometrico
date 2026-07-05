package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

// Padrão Event-Driven. Lê os callbacks do mouse/teclado de forma isolada,
// mitigando bugs da técnica de "polling" via loop update.
public class PlayerController implements InputProcessor {
    public boolean up, down, left, right;
    public boolean shiftPressed;
    public boolean ctrlPressed;
    public boolean escapePressed;

    // Triggers (Eventos de disparo único)
    private boolean rollTriggered;
    private boolean attackTriggered;
    private boolean fullscreenTriggered;
    private boolean attackPesadoTriggered = false;
    private boolean healTriggered = false;

    // Estados Isolados e Combos (ex: Segurar F3 e pressionar B)
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
            case Input.Keys.SPACE: rollTriggered = true; break;
            case Input.Keys.F11: fullscreenTriggered = true; break;
            case Input.Keys.NUM_1: healTriggered = true; break;

            // Lógica de atalhos de depuração
            case Input.Keys.F3:
                f3Held = true;
                f3ActionUsed = false;
                break;

            case Input.Keys.B:
                if (f3Held) {
                    hitboxesToggle = true;
                    f3ActionUsed = true;
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
        // Verificação para o ataque pesado
        if (button == Input.Buttons.RIGHT) {
            attackPesadoTriggered = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // --- CONSUMO DE GATILHOS (Devem ser lidos pelas lógicas via query) ---
    public boolean consumeRoll() {
        if (rollTriggered) {
            rollTriggered = false;
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

    public boolean consumeAttackPesado() {
        if (attackPesadoTriggered) {
            attackPesadoTriggered = false;
            return true;
        }
        return false;
    }

    public boolean consumeHeal() {
        if (healTriggered) {
            healTriggered = false;
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

    @Override
    public boolean keyTyped(char character) { return false; }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
