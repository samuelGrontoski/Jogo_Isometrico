package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

public class Player {
    // --- EFEITOS SONOROS ---
    private Sound somAtaque;
    private Sound somCura;
    private Sound somPassos;

    // --- CONTROLE DE PASSOS ---
    private float stepTimer = 0f;
    private float intervaloPassoAndando = 0.63f;
    private float intervaloPassoCorrendo = 0.38f;
    private float intervaloPassoAgachado = 0.63f;

    // --- STATUS DE VIDA ---
    public int vidaMaxima = 5;
    public int vida = vidaMaxima;
    public boolean isDead = false;

    // Variáveis para a animação de tomar dano (Hit Stun)
    public boolean estaTomandoDano = false;
    public float takeDamageTimer = 0f;
    public final float duracaoTakeDamage = 0.2f;

    // Variaveis basicas
    public Vector2 posicaoMundo;
    public Vector2 inputDirecao;
    public String direcaoAtual = "NW";
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
    public ParticleEffect efeitoCura;
    public boolean estaCurando = false;
    public float healTimer = 0f;
    public final float duracaoHeal = 0.7f;
    public int curasAtuais = 2;
    public final int maxCuras = 2;
    public float cooldownCuraTimer = 2f;
    public final float tempoRecargaCura = 2f;

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

    // Variaveis da cutscene ---
    public boolean emCutscene = false;
    public Vector2 destinoCutscene = new Vector2();

    float stateTime;

    Map<String, Animation<TextureRegion>> idleAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> walkAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> runAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> crouchIdleAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> crouchWalkAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> rollAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> attackLeveAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> attackPesadoAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> healAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> takeDamageAnimations = new HashMap<>();
    Map<String, Animation<TextureRegion>> deathAnimations = new HashMap<>();

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
        this.somAtaque = assets.get("sons/ataque_espada.wav", Sound.class);
        this.somCura = assets.get("sons/cura.mp3", Sound.class);
        this.somPassos = assets.get("sons/passos.wav", Sound.class);
        ParticleEffect baseEffect = assets.get("particulas/cura.p", ParticleEffect.class);
        this.efeitoCura = new ParticleEffect(baseEffect);
        this.efeitoCura.scaleEffect(0.35f);

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
        Texture takeDamageSheet = assets.get("personagem/TakeDamage.png", Texture.class);
        Texture deathSheet = assets.get("personagem/Die.png", Texture.class);

        int cols = 15;
        int rows = 8;

        idleAnimations = criarAnimacao(idleSheet, cols, rows, 0.1f, Animation.PlayMode.LOOP);
        walkAnimations = criarAnimacao(walkSheet, cols, rows, 0.08f, Animation.PlayMode.LOOP);
        runAnimations = criarAnimacao(runSheet, cols, rows, 0.05f, Animation.PlayMode.LOOP);
        crouchIdleAnimations = criarAnimacao(crouchIdleSheet, cols, rows, 0.1f, Animation.PlayMode.LOOP);
        crouchWalkAnimations = criarAnimacao(crouchWalkSheet, cols, rows, 0.08f, Animation.PlayMode.LOOP);
        rollAnimations = criarAnimacao(rollSheet, cols, rows, duracaoRoll / cols, Animation.PlayMode.NORMAL);
        attackLeveAnimations = criarAnimacao(attackLeveSheet, cols, rows, duracaoAtaque / cols, Animation.PlayMode.NORMAL);
        attackPesadoAnimations = criarAnimacao(attackPesadoSheet, cols, rows, duracaoAtaquePesado / cols, Animation.PlayMode.NORMAL);
        healAnimations = criarAnimacao(healSheet, cols, rows, duracaoHeal / cols, Animation.PlayMode.NORMAL);
        takeDamageAnimations = criarAnimacao(takeDamageSheet, cols, rows, duracaoTakeDamage / cols, Animation.PlayMode.NORMAL);
        deathAnimations = criarAnimacao(deathSheet, cols, rows, 0.15f, Animation.PlayMode.NORMAL);
    }

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
        if (isDead) return;

        // Força o cancelamento de qualquer ação (caso o player entre rolando ou atacando)
        if (emCutscene) {
            estaAtacando = false;
            estaAtacandoPesado = false;
            estaRolando = false;
            estaCurando = false;

            // Calcula a distância entre onde o player está e onde deve chegar
            Vector2 direcao = new Vector2(destinoCutscene).sub(posicaoMundo);

            // Se a distância for maior que 0.2 blocos (ainda não chegou)
            if (direcao.len() > 0.2f) {
                inputDirecao.set(direcao).nor();
                float moveSpeed = velocidadeBase * delta;
                aplicarMovimentoComColisao(moveSpeed, hitboxesMapa, hitboxBoss);
                estaEmMovimento = true;

                // Atualiza a direção que a arte deve olhar matematicamente
                float angle = MathUtils.atan2(inputDirecao.y, inputDirecao.x) * MathUtils.radiansToDegrees;
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

                // Toca o som de passos
                stepTimer += delta;
                if (stepTimer >= intervaloPassoAndando) {
                    somPassos.play(0.13f);
                    stepTimer = 0f;
                }
            } else {
                // Chegou no destino! Congela no lugar.
                estaEmMovimento = false;
                inputDirecao.set(0, 0);
            }
            atualizarHitbox();
            return;
        }

        if (cooldownRollTimer < tempoRecargaRoll) {
            cooldownRollTimer += delta;
        }
        if (cooldownCuraTimer < tempoRecargaCura) {
            cooldownCuraTimer += delta;
        }
        inputDirecao.set(0, 0);

        if (!estaAtacando && !estaAtacandoPesado && !estaRolando && !estaCurando && !estaTomandoDano) {
            lerInputsController();
        }

        if (controller.consumeRoll() && !estaTomandoDano && !estaRolando && !estaAtacando && !estaAtacandoPesado && !estaCurando && cooldownRollTimer >= tempoRecargaRoll) {
            iniciarRoll();
        }

        if (controller.consumeHeal() && !estaTomandoDano && !estaRolando && !estaAtacando && !estaAtacandoPesado && !estaCurando && curasAtuais > 0 && cooldownCuraTimer >= tempoRecargaCura) {
            iniciarHeal();
        }

        float velocidadeAtual = velocidadeBase;

        if (estaTomandoDano) {
            takeDamageTimer += delta;
            velocidadeAtual = 0f;
            estaCorrendo = false;
            estaAgachado = false;

            if (takeDamageTimer >= duracaoTakeDamage) {
                estaTomandoDano = false;
            }
            inputDirecao.set(0, 0);
            estaEmMovimento = false;

        } else if (estaCurando) {
            healTimer += delta;
            estaCorrendo = false;
            estaAgachado = false;
            if (healTimer >= duracaoHeal) estaCurando = false;
            inputDirecao.set(0, 0);
            estaEmMovimento = false;

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
        aplicarMovimentoComColisao(moveSpeed, hitboxesMapa, hitboxBoss);

        estaEmMovimento = !inputDirecao.isZero();

        if (estaEmMovimento && !estaRolando && !estaAtacando && !estaAtacandoPesado && !estaCurando && !estaTomandoDano) {
            stepTimer += delta;
            float intervaloAtual = estaCorrendo ? intervaloPassoCorrendo : (estaAgachado ? intervaloPassoAgachado : intervaloPassoAndando);

            if (stepTimer >= intervaloAtual) {
                somPassos.play(0.13f);
                stepTimer = 0f;
            }
        } else {
            stepTimer = 100f;
        }
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
        curasAtuais--;
        cooldownCuraTimer = 0f;
        somCura.play(0.5f);

        vida++;
        if (vida > vidaMaxima) {
            vida = vidaMaxima;
        }
        efeitoCura.start();
    }

    private void verificarAtaque(float mouseMundoX, float mouseMundoY) {
        if (controller.consumeAttack() && attackTimer >= tempoRecargaAtaque && !estaTomandoDano && !estaRolando && !estaAtacandoPesado && !estaCurando) {
            estaAtacando = true;
            attackTimer = 0f;
            danoAplicado = false;

            float centroPlayerX = hitbox.x + (hitbox.width / 2f);
            float centroPlayerY = hitbox.y + (hitbox.height / 2f);

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

            somAtaque.play(0.2f);
        }

        if (controller.consumeAttackPesado() && attackPesadoTimer >= tempoRecargaAtaquePesado && !estaTomandoDano && !estaRolando && !estaAtacando && !estaCurando) {
            estaAtacandoPesado = true;
            attackPesadoTimer = 0f;
            danoPesadoAplicado = false;

            float centroPlayerX = hitbox.x + (hitbox.width / 2f);
            float centroPlayerY = hitbox.y + (hitbox.height / 2f);

            Vector2 vetorMira = getVector2(mouseMundoX, mouseMundoY, centroPlayerX, centroPlayerY);

            float alcancePesado = 2.0f;
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

            somAtaque.play(0.2f, 0.70f, 0f);
        }
    }

    private Vector2 getVector2(float mouseMundoX, float mouseMundoY, float origemX, float origemY) {
        float tileW = 32f;
        float tileH = 16f;
        float a = mouseMundoX / (tileW / 2f);
        float b = mouseMundoY / (tileH / 2f);
        float mouseCartesianX = (a + b) / 2f;
        float mouseCartesianY = (b - a) / 2f;

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
            if (verificaColisoes(hitboxesMapa, hitboxBoss)) posicaoMundo.x = oldX;

            posicaoMundo.y += inputDirecao.y * moveSpeed;
            atualizarHitbox();
            if (verificaColisoes(hitboxesMapa, hitboxBoss)) posicaoMundo.y = oldY;
        }
    }

    private boolean verificaColisoes(Array<Rectangle> hitboxesMapa, Rectangle hitboxBoss) {
        for (Rectangle rect : hitboxesMapa) {
            if (hitbox.overlaps(rect)) return true;
        }

        if (hitboxBoss != null && hitbox.overlaps(hitboxBoss)) {
            return true;
        }

        return false;
    }

    public void atualizarLogicaAtaque(float delta, Array<Morcego> morcegos, Boss boss) {
        if (attackTimer < tempoRecargaAtaque) attackTimer += delta;
        if (attackTimer >= duracaoAtaque) estaAtacando = false;

        // Ataque Leve
        if (estaAtacando && !danoAplicado) {
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo && hitboxAtaque.overlaps(morcego.hitboxColisao)) {
                    morcego.tomarDano();
                }
            }
            if (boss != null && !boss.isDead && hitboxAtaque.overlaps(boss.hitbox)) {
                boss.tomarDano(1);
            }
            danoAplicado = true;
        }

        if (attackPesadoTimer < tempoRecargaAtaquePesado) attackPesadoTimer += delta;
        if (attackPesadoTimer >= duracaoAtaquePesado) estaAtacandoPesado = false;

        // Ataque Pesado
        if (estaAtacandoPesado && !danoPesadoAplicado) {
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo && hitboxAtaquePesado.overlaps(morcego.hitboxColisao)) {
                    morcego.tomarDano();
                    morcego.tomarDano();
                }
            }
            if (boss != null && !boss.isDead && hitboxAtaquePesado.overlaps(boss.hitbox)) {
                boss.tomarDano(2);
            }
            danoPesadoAplicado = true;
        }
    }

    public void tomarDano(int dano) {
        if (isDead) return;

        vida -= dano;

        if (vida <= 0) {
            vida = 0;
            isDead = true;
            stateTime = 0f;

            estaRolando = false;
            estaAtacando = false;
            estaAtacandoPesado = false;
            estaCurando = false;
            estaEmMovimento = false;
            estaTomandoDano = false;
        } else {
            estaTomandoDano = true;
            takeDamageTimer = 0f;

            // Interrompe qualquer ação que o jogador estivesse fazendo
            estaRolando = false;
            estaAtacando = false;
            estaAtacandoPesado = false;
            estaCurando = false;
            estaEmMovimento = false;
        }
    }

    public void atualizarRenderizacao(float delta, float screenX, float screenY) {
        stateTime += delta;
        TextureRegion currentFrame;

        // --- HIERARQUIA DE RENDERIZAÇÃO ---
        if (isDead) {
            Animation<TextureRegion> anim = deathAnimations.get(direcaoAtual);
            if (anim == null) anim = deathAnimations.get("SE");
            currentFrame = anim.getKeyFrame(stateTime, false);

        } else if (estaTomandoDano) {
            Animation<TextureRegion> anim = takeDamageAnimations.get(direcaoAtual);
            if (anim == null) anim = takeDamageAnimations.get("SE");
            currentFrame = anim.getKeyFrame(takeDamageTimer, false);

        } else if (estaCurando) {
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

        if (!efeitoCura.isComplete()) {
            efeitoCura.setPosition(screenX, screenY + 32f);
        }
    }

    private void atualizarHitbox() {
        hitbox.setPosition(
            posicaoMundo.x + hitbox.width,
            posicaoMundo.y + hitbox.height
        );
    }

    public boolean isAnimacaoMorteTerminada() {
        if (!isDead) return false;

        Animation<TextureRegion> anim = deathAnimations.get(direcaoAtual);
        if (anim == null) anim = deathAnimations.get("SE");

        return anim.isAnimationFinished(stateTime);
    }
}
