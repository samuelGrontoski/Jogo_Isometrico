package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player {

    // --- Física e Movimento ---
    public Vector2 posicaoMundo;
    public Vector2 inputDirecao;
    public String direcaoAtual = "SE";
    public float velocidadeBase = 7.5f;
    public boolean estaEmMovimento = false;

    // --- Colisão e Combate ---
    public Rectangle hitbox;
    public Rectangle hitboxAtaque;
    public boolean estaAtacando = false;
    public float attackTimer = 0.41f;
    public final float duracaoAtaque = 0.2f;
    public final float tempoRecargaAtaque = 0.41f;

    // --- Dash ---
    public boolean estaDandoDash = false;
    public float dashTimer = 0f;
    public final float duracaoDash = 0.15f;
    public float cooldownDashTimer = 0.5f;
    public final float tempoRecargaDash = 1.0f;
    public Vector2 direcaoDash = new Vector2();

    // --- Animações ---
    float stateTime;

    // Idle
    final int quantidade_frames_idle = 6;
    float tempo_espera_idle = 1.6f;
    float duracaoAnimacaoIdle;
    float tempo_ciclo_idle;

    Texture idleSheetSE;
    Animation<TextureRegion> idleAnimationSE;

    Texture idleSheetSW;
    Animation<TextureRegion> idleAnimationSW;

    // Run
    final int quantidade_frames_run = 6;

    Texture runSheetSE;
    Animation<TextureRegion> runAnimationSE;

    Texture runSheetSW;
    Animation<TextureRegion> runAnimationSW;

    // Pacote para o Z-Sorting
    public GameScreen.ObjetoRenderizavel renderObj;

    public Player(Vector2 posicaoInicial) {
        this.posicaoMundo = posicaoInicial;
        this.inputDirecao = new Vector2();
        this.hitbox = new Rectangle(0, 0, 0.8f, 0.8f);
        this.hitboxAtaque = new Rectangle();
        this.renderObj = new GameScreen.ObjetoRenderizavel();

        carregarAnimacoes();
        atualizarHitbox();
    }

    private void carregarAnimacoes() {
        // Idle
        idleSheetSE = new Texture("personagem/personagem_idle_se.png");
        TextureRegion[][] tmpSE = TextureRegion.split(idleSheetSE, idleSheetSE.getWidth() / quantidade_frames_idle, idleSheetSE.getHeight());
        TextureRegion[] idleFramesSE = new TextureRegion[quantidade_frames_idle];
        System.arraycopy(tmpSE[0], 0, idleFramesSE, 0, quantidade_frames_idle);
        idleAnimationSE = new Animation<>(0.1f, idleFramesSE);
        idleAnimationSE.setPlayMode(Animation.PlayMode.NORMAL);

        idleSheetSW = new Texture("personagem/personagem_idle_sw.png");
        TextureRegion[][] tmpSW = TextureRegion.split(idleSheetSW, idleSheetSW.getWidth() / quantidade_frames_idle, idleSheetSW.getHeight());
        TextureRegion[] idleFramesSW = new TextureRegion[quantidade_frames_idle];
        System.arraycopy(tmpSW[0], 0, idleFramesSW, 0, quantidade_frames_idle);
        idleAnimationSW = new Animation<>(0.1f, idleFramesSW);
        idleAnimationSW.setPlayMode(Animation.PlayMode.NORMAL);

        duracaoAnimacaoIdle = idleAnimationSE.getAnimationDuration();
        tempo_ciclo_idle = duracaoAnimacaoIdle + tempo_espera_idle;

        // Correndo
        runSheetSE = new Texture("personagem/personagem_run_se.png");
        TextureRegion[][] tmpRunSE = TextureRegion.split(runSheetSE, runSheetSE.getWidth() / quantidade_frames_run, runSheetSE.getHeight());
        TextureRegion[] runFramesSE = new TextureRegion[quantidade_frames_run];
        System.arraycopy(tmpRunSE[0], 0, runFramesSE, 0, quantidade_frames_run);
        runAnimationSE = new Animation<>(0.13f, runFramesSE);
        runAnimationSE.setPlayMode(Animation.PlayMode.LOOP);

        runSheetSW = new Texture("personagem/personagem_run_sw.png");
        TextureRegion[][] tmpRunSW = TextureRegion.split(runSheetSW, runSheetSW.getWidth() / quantidade_frames_run, runSheetSW.getHeight());
        TextureRegion[] runFramesSW = new TextureRegion[quantidade_frames_run];
        System.arraycopy(tmpRunSW[0], 0, runFramesSW, 0, quantidade_frames_run);
        runAnimationSW = new Animation<>(0.13f, runFramesSW);
        runAnimationSW.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void updateInput(float delta, Array<Pedra> pedrasDoMapa, float limiteX, float limiteY) {
        if (cooldownDashTimer < tempoRecargaDash) {
            cooldownDashTimer += delta;
        }

        inputDirecao.set(0, 0);

        if (!estaAtacando && !estaDandoDash) {
            lerTeclasMovimento();
        }

        // Dash
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !estaDandoDash && !estaAtacando && cooldownDashTimer >= tempoRecargaDash) {
            estaDandoDash = true;
            dashTimer = 0f;
            cooldownDashTimer = 0f;

            if (!inputDirecao.isZero()) {
                direcaoDash.set(inputDirecao).nor();
            } else {
                switch (direcaoAtual) {
                    case "N":
                        direcaoDash.set(1, 1);
                        break;
                    case "S":
                        direcaoDash.set(-1, -1);
                        break;
                    case "E":
                        direcaoDash.set(1, -1);
                        break;
                    case "W":
                        direcaoDash.set(-1, 1);
                        break;
                    case "NE":
                        direcaoDash.set(1, 0);
                        break;
                    case "NW":
                        direcaoDash.set(0, 1);
                        break;
                    case "SE":
                        direcaoDash.set(0, -1);
                        break;
                    case "SW":
                        direcaoDash.set(-1, 0);
                        break;
                }
                direcaoDash.nor();
            }
        }

        float velocidadeAtual = velocidadeBase;

        if (estaDandoDash) {
            dashTimer += delta;
            velocidadeAtual = 25f;
            inputDirecao.set(direcaoDash);

            if (dashTimer >= duracaoDash) {
                estaDandoDash = false;
            }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                velocidadeAtual = 10f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                velocidadeAtual = 2.5f;
            }
        }

        float moveSpeed = velocidadeAtual * delta;

        verificarAtaque();
        aplicarMovimentoComColisao(moveSpeed, pedrasDoMapa);
        restringirAosLimitesDoMapa(limiteX, limiteY);

        estaEmMovimento = !inputDirecao.isZero();
    }

    private void lerTeclasMovimento() {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            inputDirecao.x += 1;
            inputDirecao.y += 1;
            direcaoAtual = "N";
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            inputDirecao.x -= 1;
            inputDirecao.y -= 1;
            direcaoAtual = "S";
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            inputDirecao.x += 1;
            inputDirecao.y -= 1;
            direcaoAtual = "E";
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            inputDirecao.x -= 1;
            inputDirecao.y += 1;
            direcaoAtual = "W";
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            direcaoAtual = "NE";
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            direcaoAtual = "NW";
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            direcaoAtual = "SE";
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            direcaoAtual = "SW";
        }
    }

    private void verificarAtaque() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && attackTimer >= tempoRecargaAtaque) {
            estaAtacando = true;
            attackTimer = 0f;

            float attackCenterX = posicaoMundo.x;
            float attackCenterY = posicaoMundo.y;
            float alcance = 1f;

            switch (direcaoAtual) {
                case "N":
                    attackCenterX += alcance;
                    attackCenterY += alcance ;
                    break;
                case "S":
                    attackCenterX -= alcance;
                    attackCenterY -= alcance;
                    break;
                case "E":
                    attackCenterX += alcance;
                    attackCenterY -= alcance;
                    break;
                case "W":
                    attackCenterX -= alcance;
                    attackCenterY += alcance;
                    break;
                case "NE":
                    attackCenterX += alcance + 0.5f;
                    attackCenterY -= alcance - 1f;
                    break;
                case "NW":
                    attackCenterX -= alcance - 1f;
                    attackCenterY += alcance + 0.5f;
                    break;
                case "SE":
                    attackCenterX -= alcance - 1f;
                    attackCenterY -= alcance + 0.5f;
                    break;
                case "SW":
                    attackCenterX -= alcance + 0.5f;
                    attackCenterY -= alcance - 1f;
                    break;
            }
            hitboxAtaque.set(attackCenterX, attackCenterY, 1.5f, 1.5f);
        }
    }

    private void aplicarMovimentoComColisao(float moveSpeed, Array<Pedra> pedrasDoMapa) {
        if (!inputDirecao.isZero()) {
            inputDirecao.nor();
            float oldX = posicaoMundo.x;
            float oldY = posicaoMundo.y;

            // X
            posicaoMundo.x += inputDirecao.x * moveSpeed;
            atualizarHitbox();
            for (Pedra pedra : pedrasDoMapa) {
                if (hitbox.overlaps(pedra.hitboxColisao)) {
                    posicaoMundo.x = oldX;
                    atualizarHitbox();
                    break;
                }
            }

            // Y
            posicaoMundo.y += inputDirecao.y * moveSpeed;
            atualizarHitbox();
            for (Pedra pedra : pedrasDoMapa) {
                if (hitbox.overlaps(pedra.hitboxColisao)) {
                    posicaoMundo.y = oldY;
                    atualizarHitbox();
                    break;
                }
            }
        }
    }

    private void restringirAosLimitesDoMapa(float limiteX, float limiteY) {
        float margemPlayer = 1.5f;
        posicaoMundo.x = MathUtils.clamp(posicaoMundo.x, 0, limiteY - margemPlayer);
        posicaoMundo.y = MathUtils.clamp(posicaoMundo.y, -limiteX, -margemPlayer);
    }

    public void atualizarLogicaAtaque(float delta, Array<Pedra> pedrasDoMapa) {
        // O relógio sempre roda até atingir o tempo de recarga
        if (attackTimer < tempoRecargaAtaque) {
            attackTimer += delta;
        }

        // Se o tempo passou da duração do ataque (0.2s), desliga a hitbox e o "lock" de movimento
        if (attackTimer >= duracaoAtaque) {
            estaAtacando = false;
        }

        // A lógica de quebrar pedra só funciona enquanto está ativamente atacando (nos primeiros 0.2s)
        if (estaAtacando) {
            for (int i = pedrasDoMapa.size - 1; i >= 0; i--) {
                if (hitboxAtaque.overlaps(pedrasDoMapa.get(i).hitboxColisao)) {
                    pedrasDoMapa.removeIndex(i);
                }
            }
        }
    }

    public void atualizarRenderizacao(float delta, float screenX, float screenY) {
        stateTime += delta;

        TextureRegion currentFrame;

        if (estaEmMovimento) {
            Animation<TextureRegion> animacaoRunCerta = (direcaoAtual.equals("SW") || direcaoAtual.equals("S") || direcaoAtual.equals("NW") || direcaoAtual.equals("W")) ? runAnimationSW : runAnimationSE;
            currentFrame = animacaoRunCerta.getKeyFrame(stateTime, true);
        } else {
            Animation<TextureRegion> animacaoIdleCerta = (direcaoAtual.equals("SW") || direcaoAtual.equals("S") || direcaoAtual.equals("NW") || direcaoAtual.equals("W")) ? idleAnimationSW : idleAnimationSE;
            if (stateTime > tempo_ciclo_idle) {
                stateTime = 0f;
            }
            if (stateTime <= duracaoAnimacaoIdle) {
                currentFrame = animacaoIdleCerta.getKeyFrame(stateTime, false);
            }
            else {
                currentFrame = animacaoIdleCerta.getKeyFrame(0, false);
            }
        }

        renderObj.textura = currentFrame;
        renderObj.drawX = screenX - (currentFrame.getRegionWidth() / 2f);
        renderObj.drawY = screenY;
        renderObj.sortY = screenY;
    }

    private void atualizarHitbox() {
        hitbox.setPosition(posicaoMundo.x + (hitbox.width / 2f), posicaoMundo.y + (hitbox.height / 2f));
    }

    public void dispose() {
        idleSheetSE.dispose();
        idleSheetSW.dispose();
        runSheetSE.dispose();
        runSheetSW.dispose();
    }
}
