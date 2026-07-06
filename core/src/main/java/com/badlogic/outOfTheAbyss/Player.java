package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

public class Player {
    // Variaveis basicas
    public Vector2 posicaoMundo;
    public Vector2 inputDirecao;
    public String direcaoAtual = "SE";
    public float velocidadeBase = 7.5f;

    // Controles de estado
    public boolean estaEmMovimento = false;
    public boolean estaCorrendo = false;
    public boolean estaAgachado = false;
    public boolean estaAtacando = false;
    private boolean danoAplicado = false;
    public boolean estaAtacandoPesado = false;
    private boolean danoPesadoAplicado = false;

    // --- STATUS DA CURA ---
    public boolean estaCurando = false;
    public float healTimer = 0f;
    public final float duracaoHeal = 0.7f;

    public Rectangle hitbox;
    public Rectangle hitboxAtaque;
    public Rectangle hitboxAtaquePesado;

    // Variaveis de ataque
    public float attackTimer = 0.41f;
    public final float duracaoAtaque = 0.5f;
    public final float tempoRecargaAtaque = 0.55f;
    public float attackPesadoTimer = 2.0f;
    public final float duracaoAtaquePesado = 0.6f;
    public final float tempoRecargaAtaquePesado = 2.0f;

    // Variaveis de esquiva
    public boolean estaRolando = false;
    public float rollTimer = 0f;
    public final float duracaoRoll = 0.6f;
    public float cooldownRollTimer = 0.6f;
    public final float tempoRecargaRoll = 1.5f;
    public Vector2 direcaoRoll = new Vector2();

    float stateTime;

    // Dicionários para guardar as 8 direções de cada estado
    Map<String, Animation<TextureRegion>> idleAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> walkAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> runAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> crouchIdleAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> crouchWalkAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> rollAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> attackLeveAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> attackPesadoAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> healAnimations = new HashMap<>();

    public ObjetoRenderizavel renderObj;
    private final PlayerController controller;

    public Player(Vector2 posicaoInicial, AssetManager assets, PlayerController controller) {
        this.posicaoMundo = posicaoInicial;
        this.inputDirecao = new Vector2();
        this.hitbox = new Rectangle(0, 0, 1.6f, 1.6f);
        this.hitboxAtaque = new Rectangle();
        this.hitboxAtaquePesado = new Rectangle();
        this.renderObj = new ObjetoRenderizavel();
        this.controller = controller;

        carregarAnimacoes(assets);
        atualizarHitbox();
    }

    private void carregarAnimacoes(AssetManager assets) {
        Texture idleSheet = assets.get("personagem/Idle.png", Texture.class);
        Texture walkSheet = assets.get("personagem/Walk.png", Texture.class);
        Texture runSheet = assets.get("personagem/Run.png", Texture.class);
        Texture crouchIdleSheet = assets.get("personagem/CrouchIdle.png", Texture.class);
        Texture crouchWalkSheet = assets.get("personagem/CrouchWalk.png", Texture.class);
        Texture rollSheet = assets.get("personagem/Rolling.png", Texture.class);
        Texture attackLeveSheet = assets.get("personagem/Melee1.png", Texture.class);
        Texture attackPesadoSheet = assets.get("personagem/Melee2.png", Texture.class);
        Texture healSheet = assets.get("personagem/Heal.png", Texture.class);

        int cols = 15; // 15 frames por animação
        int rows = 8;  // 8 direções

        idleAnimations = criarAnimacao(idleSheet, cols, rows, 0.1f, Animation.PlayMode.LOOP);
        walkAnimations = criarAnimacao(walkSheet, cols, rows, 0.08f, Animation.PlayMode.LOOP);
        runAnimations = criarAnimacao(runSheet, cols, rows, 0.05f, Animation.PlayMode.LOOP);
        crouchIdleAnimations = criarAnimacao(crouchIdleSheet, cols, rows, 0.1f, Animation.PlayMode.LOOP);
        crouchWalkAnimations = criarAnimacao(crouchWalkSheet, cols, rows, 0.08f, Animation.PlayMode.LOOP);
        rollAnimations = criarAnimacao(rollSheet, cols, rows, duracaoRoll / cols, Animation.PlayMode.NORMAL);
        attackLeveAnimations = criarAnimacao(attackLeveSheet, cols, rows, duracaoAtaque / cols, Animation.PlayMode.NORMAL);
        attackPesadoAnimations = criarAnimacao(attackPesadoSheet, cols, rows, duracaoAtaquePesado / cols, Animation.PlayMode.NORMAL);
        healAnimations = criarAnimacao(healSheet, cols, rows, duracaoHeal / cols, Animation.PlayMode.NORMAL);
    }

    // Função universal que mapeia qualquer Spritesheet padronizado e devolve o Dicionário pronto
    private Map<String, Animation<TextureRegion>> criarAnimacao(Texture sheet, int cols, int rows, float frameDuration, Animation.PlayMode mode) {
        Map<String, Animation<TextureRegion>> mapaAnimacoes = new HashMap<>();
        String[] direcoesOrdem = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};

        int larguraFrame = sheet.getWidth() / cols;
        int alturaFrame = sheet.getHeight() / rows;
        TextureRegion[][] tmp = TextureRegion.split(sheet, larguraFrame, alturaFrame);

        for (int i = 0; i < rows; i++) {
            TextureRegion[] framesLinha = new TextureRegion[cols];
            System.arraycopy(tmp[i], 0, framesLinha, 0, cols);
            Animation<TextureRegion> anim = new Animation<>(frameDuration, framesLinha);
            anim.setPlayMode(mode);
            mapaAnimacoes.put(direcoesOrdem[i], anim);
        }
        return mapaAnimacoes;
    }

    public void updateInput(float delta, Array<Rectangle> hitboxesMapa, Rectangle hitboxBoss, float limiteX, float limiteY, float mouseMundoX, float mouseMundoY) {
        if (cooldownRollTimer < tempoRecargaRoll) {
            cooldownRollTimer += delta;
        }
        inputDirecao.set(0, 0);

        if (!estaAtacando && !estaAtacandoPesado && !estaRolando && !estaCurando) {
            lerInputsController();
        }

        if (controller.consumeRoll() && !estaRolando && !estaAtacando && !estaAtacandoPesado && !estaCurando && cooldownRollTimer >= tempoRecargaRoll) {
            iniciarRoll();
        }

        if (controller.consumeHeal() && !estaRolando && !estaAtacando && !estaAtacandoPesado && !estaCurando) {
            iniciarHeal();
        }

        float velocidadeAtual = velocidadeBase;

        // --- MÁQUINA DE ESTADO ---
        if (estaCurando) {
            healTimer += delta;
            velocidadeAtual = 0f;
            estaCorrendo = false;
            estaAgachado = false;
            if (healTimer >= duracaoHeal) estaCurando = false;

        } else if (estaRolando) {
            rollTimer += delta;
            velocidadeAtual = 14f;
            inputDirecao.set(direcaoRoll);
            estaCorrendo = false;
            estaAgachado = false;
            if (rollTimer >= duracaoRoll) estaRolando = false;

        } else {
            if (controller.ctrlPressed) {
                velocidadeAtual = 2.5f;
                estaCorrendo = false;
                estaAgachado = true;
            } else if (controller.shiftPressed) {
                velocidadeAtual = 10f;
                estaCorrendo = true;
                estaAgachado = false;
            } else {
                velocidadeAtual = velocidadeBase;
                estaCorrendo = false;
                estaAgachado = false;
            }
        }

        float moveSpeed = velocidadeAtual * delta;

        verificarAtaque(mouseMundoX, mouseMundoY);

        // Repasse o hitboxBoss para a função de movimento
        aplicarMovimentoComColisao(moveSpeed, hitboxesMapa, hitboxBoss);

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

    private void iniciarRoll() {
        estaRolando = true;
        rollTimer = 0f;
        cooldownRollTimer = 0f;

        if (!inputDirecao.isZero()) {
            direcaoRoll.set(inputDirecao).nor();
        } else {
            switch (direcaoAtual) {
                case "N": direcaoRoll.set(1, 1); break;
                case "S": direcaoRoll.set(-1, -1); break;
                case "E": direcaoRoll.set(1, -1); break;
                case "W": direcaoRoll.set(-1, 1); break;
                case "NE": direcaoRoll.set(1, 0); break;
                case "NW": direcaoRoll.set(0, 1); break;
                case "SE": direcaoRoll.set(0, -1); break;
                case "SW": direcaoRoll.set(-1, 0); break;
            }
            direcaoRoll.nor();
        }
    }

    private void iniciarHeal() {
        estaCurando = true;
        healTimer = 0f;
    }

    private void verificarAtaque(float mouseMundoX, float mouseMundoY) {
        if (controller.consumeAttack() && attackTimer >= tempoRecargaAtaque && !estaRolando && !estaAtacandoPesado && !estaCurando) {
            estaAtacando = true;
            attackTimer = 0f;
            danoAplicado = false;

            // 1. Descobrir o centro real da Hitbox do Player
            float centroPlayerX = hitbox.x + (hitbox.width / 2f);
            float centroPlayerY = hitbox.y + (hitbox.height / 2f);

            // 2. Mirar a partir do centro real
            Vector2 vetorMira = getVector2(mouseMundoX, mouseMundoY, centroPlayerX, centroPlayerY);

            float alcanceLeve = 2.0f;
            float larguraHitboxLeve = 2f;
            float alturaHitboxLeve = 2f;

            float attackCenterX = centroPlayerX + (vetorMira.x * alcanceLeve);
            float attackCenterY = centroPlayerY + (vetorMira.y * alcanceLeve);

            hitboxAtaque.set(
                attackCenterX - (larguraHitboxLeve / 2f),
                attackCenterY - (alturaHitboxLeve / 2f),
                larguraHitboxLeve,
                alturaHitboxLeve
            );
        }

        if (controller.consumeAttackPesado() && attackPesadoTimer >= tempoRecargaAtaquePesado && !estaRolando && !estaAtacando && !estaCurando) {
            estaAtacandoPesado = true;
            attackPesadoTimer = 0f;
            danoPesadoAplicado = false;

            // 1. Descobrir o centro real da Hitbox do Player
            float centroPlayerX = hitbox.x + (hitbox.width / 2f);
            float centroPlayerY = hitbox.y + (hitbox.height / 2f);

            // 2. Mirar a partir do centro real
            Vector2 vetorMira = getVector2(mouseMundoX, mouseMundoY, centroPlayerX, centroPlayerY);

            float alcancePesado = 2.0f; // Ataque pesado precisa ir mais longe para ficar fora da hitbox
            float larguraHitboxPesado = 2.4f;
            float alturaHitboxPesado = 2.4f;

            float attackCenterX = centroPlayerX + (vetorMira.x * alcancePesado);
            float attackCenterY = centroPlayerY + (vetorMira.y * alcancePesado);

            hitboxAtaquePesado.set(
                attackCenterX - (larguraHitboxPesado / 2f),
                attackCenterY - (alturaHitboxPesado / 2f),
                larguraHitboxPesado,
                alturaHitboxPesado
            );
        }
    }

    // Mira usada nos ataques leve e pesado
    private Vector2 getVector2(float mouseMundoX, float mouseMundoY, float origemX, float origemY) {
        float tileW = 32f;
        float tileH = 16f;
        float a = mouseMundoX / (tileW / 2f);
        float b = mouseMundoY / (tileH / 2f);
        float mouseCartesianX = (a + b) / 2f;
        float mouseCartesianY = (b - a) / 2f;

        // O ângulo agora leva em consideração a verdadeira origem do corpo do jogador
        float deltaX = mouseCartesianX - origemX;
        float deltaY = mouseCartesianY - origemY;

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
        return vetorMira;
    }

    private void aplicarMovimentoComColisao(float moveSpeed, Array<Rectangle> hitboxesMapa, Rectangle hitboxBoss) {
        if (!inputDirecao.isZero()) {
            inputDirecao.nor();
            float oldX = posicaoMundo.x;
            float oldY = posicaoMundo.y;

            posicaoMundo.x += inputDirecao.x * moveSpeed;
            atualizarHitbox();
            // Agora verifica colisão geral (mapa + boss)
            if (verificaColisoes(hitboxesMapa, hitboxBoss)) posicaoMundo.x = oldX;

            posicaoMundo.y += inputDirecao.y * moveSpeed;
            atualizarHitbox();
            // Agora verifica colisão geral (mapa + boss)
            if (verificaColisoes(hitboxesMapa, hitboxBoss)) posicaoMundo.y = oldY;
        }
    }

    private boolean verificaColisoes(Array<Rectangle> hitboxesMapa, Rectangle hitboxBoss) {
        // 1. Verifica colisão com as paredes/mapa
        for (Rectangle rect : hitboxesMapa) {
            if (hitbox.overlaps(rect)) return true;
        }

        // 2. Verifica colisão com o corpo do boss
        // (A checagem != null previne crash caso o boss ainda não tenha spawnado ou já tenha morrido)
        if (hitboxBoss != null && hitbox.overlaps(hitboxBoss)) {
            return true;
        }

        return false;
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

        // Ataque pesado
        if (attackPesadoTimer < tempoRecargaAtaquePesado) attackPesadoTimer += delta;
        if (attackPesadoTimer >= duracaoAtaquePesado) estaAtacandoPesado = false;

        if (estaAtacandoPesado && !danoPesadoAplicado) {
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo && hitboxAtaquePesado.overlaps(morcego.hitboxColisao)) {
                    morcego.tomarDano(); // Aplique x2 caso queira dar o dobro de dano
                    morcego.tomarDano();
                }
            }
            danoPesadoAplicado = true;
        }
    }

    public void atualizarRenderizacao(float delta, float screenX, float screenY) {
        stateTime += delta;
        TextureRegion currentFrame;

        // PRIORIDADE DE RENDERIZAÇÃO
        if (estaCurando) {
            Animation<TextureRegion> anim = healAnimations.get(direcaoAtual);
            if (anim == null) anim = healAnimations.get("SE");
            currentFrame = anim.getKeyFrame(healTimer, false);
        } else if (estaRolando) {
            Animation<TextureRegion> anim = rollAnimations.get(direcaoAtual);
            if (anim == null) anim = rollAnimations.get("SE");
            currentFrame = anim.getKeyFrame(rollTimer, false);
        } else if (estaAtacandoPesado) {
            Animation<TextureRegion> anim = attackPesadoAnimations.get(direcaoAtual);
            if (anim == null) anim = attackPesadoAnimations.get("SE");
            currentFrame = anim.getKeyFrame(attackPesadoTimer, false);
        } else if (estaAtacando) {
            Animation<TextureRegion> anim = attackLeveAnimations.get(direcaoAtual);
            if (anim == null) anim = attackLeveAnimations.get("SE");
            currentFrame = anim.getKeyFrame(attackTimer, false);
        } else if (estaEmMovimento) {
            Animation<TextureRegion> animacaoMovimento;
            if (estaAgachado) {
                animacaoMovimento = crouchWalkAnimations.get(direcaoAtual);
                if (animacaoMovimento == null) animacaoMovimento = crouchWalkAnimations.get("SE");
            } else if (estaCorrendo) {
                animacaoMovimento = runAnimations.get(direcaoAtual);
                if (animacaoMovimento == null) animacaoMovimento = runAnimations.get("SE");
            } else {
                animacaoMovimento = walkAnimations.get(direcaoAtual);
                if (animacaoMovimento == null) animacaoMovimento = walkAnimations.get("SE");
            }
            currentFrame = animacaoMovimento.getKeyFrame(stateTime, true);
        } else {
            Animation<TextureRegion> animacaoIdleCerta;
            if (estaAgachado) {
                animacaoIdleCerta = crouchIdleAnimations.get(direcaoAtual);
                if (animacaoIdleCerta == null) animacaoIdleCerta = crouchIdleAnimations.get("SE");
            } else {
                animacaoIdleCerta = idleAnimations.get(direcaoAtual);
                if (animacaoIdleCerta == null) animacaoIdleCerta = idleAnimations.get("SE");
            }
            currentFrame = animacaoIdleCerta.getKeyFrame(stateTime, true);
        }

        // --- SISTEMA DE ESCALA VISUAL (80%) ---
        float escalaVisual = 0.8f;

        renderObj.textura = currentFrame;
        renderObj.isTransformado = true;
        renderObj.width = currentFrame.getRegionWidth();
        renderObj.height = currentFrame.getRegionHeight();

        renderObj.originX = renderObj.width / 2f;
        renderObj.originY = 0f;

        renderObj.scaleX = escalaVisual;
        renderObj.scaleY = escalaVisual;
        renderObj.grausRotacao = 0f;

        renderObj.drawX = screenX - renderObj.originX;
        renderObj.drawY = screenY;
        renderObj.sortY = screenY;
    }

    private void atualizarHitbox() {
        hitbox.setPosition(
            posicaoMundo.x + hitbox.width,
            posicaoMundo.y + hitbox.height
        );
    }
}
