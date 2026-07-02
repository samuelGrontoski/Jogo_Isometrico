package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player {
    public Vector2 posicaoMundo;
    public Vector2 inputDirecao;
    public String direcaoAtual = "SE";
    public float velocidadeBase = 7.5f;
    public boolean estaEmMovimento = false;

    public Rectangle hitbox;
    public Rectangle hitboxAtaque;

    public boolean estaAtacando = false;
    private boolean danoAplicado = false;

    public float attackTimer = 0.41f;
    public final float duracaoAtaque = 0.2f;
    public final float tempoRecargaAtaque = 0.41f;

    public boolean estaDandoDash = false;
    public float dashTimer = 0f;
    public final float duracaoDash = 0.15f;
    public float cooldownDashTimer = 0.5f;
    public final float tempoRecargaDash = 1.0f;
    public Vector2 direcaoDash = new Vector2();

    float stateTime;
    final int quantidade_frames_idle = 6;
    float tempo_espera_idle = 1.6f;
    float duracaoAnimacaoIdle;
    float tempo_ciclo_idle;

    Animation<TextureRegion> idleAnimationSE;
    Animation<TextureRegion> idleAnimationSW;
    Animation<TextureRegion> runAnimationSE;
    Animation<TextureRegion> runAnimationSW;

    public ObjetoRenderizavel renderObj;
    private final PlayerController controller;

    public Player(Vector2 posicaoInicial, AssetManager assets, PlayerController controller) {
        this.posicaoMundo = posicaoInicial;
        this.inputDirecao = new Vector2();
        this.hitbox = new Rectangle(0, 0, 0.8f, 0.8f);
        this.hitboxAtaque = new Rectangle();
        this.renderObj = new ObjetoRenderizavel();
        this.controller = controller;

        carregarAnimacoes(assets);
        atualizarHitbox();
    }

    private void carregarAnimacoes(AssetManager assets) {
        Texture idleSheetSE = assets.get("personagem/personagem_idle_se.png", Texture.class);
        idleAnimationSE = criarAnimacao(idleSheetSE, quantidade_frames_idle, 0.1f, Animation.PlayMode.NORMAL);

        Texture idleSheetSW = assets.get("personagem/personagem_idle_sw.png", Texture.class);
        idleAnimationSW = criarAnimacao(idleSheetSW, quantidade_frames_idle, 0.1f, Animation.PlayMode.NORMAL);

        duracaoAnimacaoIdle = idleAnimationSE.getAnimationDuration();
        tempo_ciclo_idle = duracaoAnimacaoIdle + tempo_espera_idle;

        int quantidade_frames_run = 6;
        Texture runSheetSE = assets.get("personagem/personagem_run_se.png", Texture.class);
        runAnimationSE = criarAnimacao(runSheetSE, quantidade_frames_run, 0.13f, Animation.PlayMode.LOOP);

        Texture runSheetSW = assets.get("personagem/personagem_run_sw.png", Texture.class);
        runAnimationSW = criarAnimacao(runSheetSW, quantidade_frames_run, 0.13f, Animation.PlayMode.LOOP);
    }

    private Animation<TextureRegion> criarAnimacao(Texture sheet, int frames, float frameDuration, Animation.PlayMode mode) {
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / frames, sheet.getHeight());
        TextureRegion[] animationFrames = new TextureRegion[frames];
        System.arraycopy(tmp[0], 0, animationFrames, 0, frames);
        Animation<TextureRegion> anim = new Animation<>(frameDuration, animationFrames);
        anim.setPlayMode(mode);
        return anim;
    }

    public void updateInput(float delta, Array<Pedra> pedrasDoMapa, Array<Rectangle> hitboxesMapa, float limiteX, float limiteY, float mouseMundoX, float mouseMundoY) {
        if (cooldownDashTimer < tempoRecargaDash) {
            cooldownDashTimer += delta;
        }
        inputDirecao.set(0, 0);

        if (!estaAtacando && !estaDandoDash) {
            lerInputsController();
        }

        if (controller.consumeDash() && !estaDandoDash && !estaAtacando && cooldownDashTimer >= tempoRecargaDash) {
            iniciarDash();
        }

        float velocidadeAtual = velocidadeBase;
        if (estaDandoDash) {
            dashTimer += delta;
            velocidadeAtual = 25f;
            inputDirecao.set(direcaoDash);
            if (dashTimer >= duracaoDash) estaDandoDash = false;
        } else {
            if (controller.shiftPressed) velocidadeAtual = 10f;
            else if (controller.ctrlPressed) velocidadeAtual = 2.5f;
        }

        float moveSpeed = velocidadeAtual * delta;

        verificarAtaque(mouseMundoX, mouseMundoY);
        // Aplica e verifica a colisão tanto contra as pedras, quanto contra o array de blocos magnéticos lidos do TMX
        aplicarMovimentoComColisao(moveSpeed, pedrasDoMapa, hitboxesMapa);

        restringirAosLimitesDoMapa(limiteX, limiteY);

        estaEmMovimento = !inputDirecao.isZero();
    }

    private void lerInputsController() {
        if (controller.up) { inputDirecao.add(1, 1); direcaoAtual = "N"; }
        if (controller.down) { inputDirecao.add(-1, -1); direcaoAtual = "S"; }
        if (controller.right) { inputDirecao.add(1, -1); direcaoAtual = "E"; }
        if (controller.left) { inputDirecao.add(-1, 1); direcaoAtual = "W"; }

        if (controller.up && controller.right) direcaoAtual = "NE";
        else if (controller.up && controller.left) direcaoAtual = "NW";
        else if (controller.down && controller.right) direcaoAtual = "SE";
        else if (controller.down && controller.left) direcaoAtual = "SW";
    }

    private void iniciarDash() {
        estaDandoDash = true;
        dashTimer = 0f;
        cooldownDashTimer = 0f;

        if (!inputDirecao.isZero()) {
            direcaoDash.set(inputDirecao).nor();
        } else {
            switch (direcaoAtual) {
                case "N": direcaoDash.set(1, 1); break;
                case "S": direcaoDash.set(-1, -1); break;
                case "E": direcaoDash.set(1, -1); break;
                case "W": direcaoDash.set(-1, 1); break;
                case "NE": direcaoDash.set(1, 0); break;
                case "NW": direcaoDash.set(0, 1); break;
                case "SE": direcaoDash.set(0, -1); break;
                case "SW": direcaoDash.set(-1, 0); break;
            }
            direcaoDash.nor();
        }
    }

    private void verificarAtaque(float mouseMundoX, float mouseMundoY) {
        if (controller.consumeAttack() && attackTimer >= tempoRecargaAtaque) {
            estaAtacando = true;
            attackTimer = 0f;
            danoAplicado = false;

            float tileW = 32f;
            float tileH = 16f;
            float a = mouseMundoX / (tileW / 2f);
            float b = mouseMundoY / (tileH / 2f);
            float mouseCartesianX = (a + b) / 2f;
            float mouseCartesianY = (b - a) / 2f;

            float deltaX = mouseCartesianX - posicaoMundo.x;
            float deltaY = mouseCartesianY - posicaoMundo.y;

            float angle = MathUtils.atan2(deltaY, deltaX) * MathUtils.radiansToDegrees;
            if (angle < 0) angle += 360f;

            int index = MathUtils.floor((angle + 22.5f) / 45f) % 8;
            switch (index) {
                case 0: direcaoAtual = "NE"; break;
                case 1: direcaoAtual = "N";  break;
                case 2: direcaoAtual = "NW"; break;
                case 3: direcaoAtual = "W";  break;
                case 4: direcaoAtual = "SW"; break;
                case 5: direcaoAtual = "S";  break;
                case 6: direcaoAtual = "SE"; break;
                case 7: direcaoAtual = "E";  break;
            }

            Vector2 vetorMira = new Vector2(deltaX, deltaY);
            if (!vetorMira.isZero()) {
                vetorMira.nor();
            } else {
                vetorMira.set(1, 0);
            }

            float alcance = 1.5f;
            float attackCenterX = posicaoMundo.x + (vetorMira.x * alcance);
            float attackCenterY = posicaoMundo.y + (vetorMira.y * alcance);

            hitboxAtaque.set(attackCenterX, attackCenterY, 2f, 2f);
        }
    }

    private void aplicarMovimentoComColisao(float moveSpeed, Array<Pedra> pedrasDoMapa, Array<Rectangle> hitboxesMapa) {
        if (!inputDirecao.isZero()) {
            inputDirecao.nor();
            float oldX = posicaoMundo.x;
            float oldY = posicaoMundo.y;

            posicaoMundo.x += inputDirecao.x * moveSpeed;
            atualizarHitbox();
            if (verificaColisao(pedrasDoMapa) || verificaColisaoMapa(hitboxesMapa)) posicaoMundo.x = oldX;

            posicaoMundo.y += inputDirecao.y * moveSpeed;
            atualizarHitbox();
            if (verificaColisao(pedrasDoMapa) || verificaColisaoMapa(hitboxesMapa)) posicaoMundo.y = oldY;
        }
    }

    private boolean verificaColisao(Array<Pedra> pedras) {
        for (Pedra p : pedras) {
            if (hitbox.overlaps(p.hitboxColisao)) return true;
        }
        return false;
    }

    private boolean verificaColisaoMapa(Array<Rectangle> hitboxesMapa) {
        for (Rectangle rect : hitboxesMapa) {
            if (hitbox.overlaps(rect)) return true;
        }
        return false;
    }

    private void restringirAosLimitesDoMapa(float limiteX, float limiteY) {
    }

    public void atualizarLogicaAtaque(float delta, Array<Morcego> morcegos) {
        if (attackTimer < tempoRecargaAtaque) attackTimer += delta;
        if (attackTimer >= duracaoAtaque) estaAtacando = false;

        if (estaAtacando && !danoAplicado) {
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo && hitboxAtaque.overlaps(morcego.hitboxColisao)) {
                    morcego.tomarDano();
                }
            }
            danoAplicado = true;
        }
    }

    public void atualizarRenderizacao(float delta, float screenX, float screenY) {
        stateTime += delta;
        TextureRegion currentFrame;
        boolean usaSW = direcaoAtual.equals("SW") || direcaoAtual.equals("S") || direcaoAtual.equals("NW") || direcaoAtual.equals("W");

        if (estaEmMovimento) {
            Animation<TextureRegion> animacaoRunCerta = usaSW ? runAnimationSW : runAnimationSE;
            currentFrame = animacaoRunCerta.getKeyFrame(stateTime, true);
        } else {
            Animation<TextureRegion> animacaoIdleCerta = usaSW ? idleAnimationSW : idleAnimationSE;
            if (stateTime > tempo_ciclo_idle) stateTime = 0f;
            currentFrame = stateTime <= duracaoAnimacaoIdle ? animacaoIdleCerta.getKeyFrame(stateTime, false) : animacaoIdleCerta.getKeyFrame(0, false);
        }

        renderObj.textura = currentFrame;
        renderObj.drawX = screenX - (currentFrame.getRegionWidth() / 2f);
        renderObj.drawY = screenY;
        renderObj.sortY = screenY;
    }

    private void atualizarHitbox() {
        hitbox.setPosition(posicaoMundo.x + (hitbox.width / 2f), posicaoMundo.y + (hitbox.height / 2f));
    }
}
