package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Boss {
    public int vidaMaxima = 10;
    public int vida = vidaMaxima;
    public boolean isDead = false;
    private boolean isBlinking = false;
    private float blinkTimer = 0f;
    public Vector2 position;
    public float speed = 7f;
    public float aggroRange = 25f;
    public float attackRange = 6f;
    public String currentState = "IDLE";

    public Rectangle hitbox;
    public Rectangle hitboxAtaque;

    public ObjetoRenderizavel renderObj;

    private Animation<TextureRegion> idleSEAnimation;
    private Animation<TextureRegion> idleSWAnimation;
    private Animation<TextureRegion> walkSEAnimation;
    private Animation<TextureRegion> walkSWAnimation;
    private Animation<TextureRegion> attackSEAnimation;
    private Animation<TextureRegion> attackSWAnimation;
    private Animation<TextureRegion> deathSEAnimation;
    private Animation<TextureRegion> deathSWAnimation;
    private float stateTime;
    private String direcaoAtual = "SE";

    private Array<Vector2> caminhoAtual;
    private int indiceCaminho = 0;
    private float timerRecalculoCaminho = 0f;
    private final float intervaloRecalculo = 0.5f;

    private boolean usandoPathfinding = false;
    private float timerTrocaModo = 0f;
    private final float cooldownTrocaModo = 0.4f;

    private float timerTrocaDirecao = 0f;
    private final float cooldownTrocaDirecao = 0.15f;

    // --- SISTEMA DE ATAQUE ---
    private float attackStateTime = 0f;
    private boolean danoAplicado = false;
    private boolean precisaReiniciarAtaque = false;
    private final float PROGRESSO_IMPACTO_INICIO = 0.55f;
    private final float PROGRESSO_IMPACTO_FIM = 0.75f;

    private final Array<Vector2> tilesAtaqueTelegraph = new Array<>();
    private final Vector2 direcaoVetorAtaque = new Vector2(1, 0);
    private final Vector2 alvoAtaqueTravar = new Vector2();
    private float attackCooldownTimer = 0f;
    public float attackCooldown = 0.5f;

    // Ataque de Teia
    public float rangedAttackRange = 12f;
    private float timerCooldownRanged = 0f;
    private final float COOLDOWN_RANGED = 3.0f;

    private Animation<TextureRegion> attack2SEAnimation;
    private Animation<TextureRegion> attack2SWAnimation;

    private Texture sheetProjetilSE;
    private Texture sheetProjetilSW;

    public Array<TeiaProjetil> teiasAtivas = new Array<>();
    private boolean projetilDisparado = false;

    private static final float TILE_WIDTH = 32f;
    private static final float TILE_HEIGHT = 16f;

    // --- ÁUDIO ---
    private Sound somMorte;

    public Boss(Vector2 spawn, AssetManager assets) {
        position = spawn;
        this.hitbox = new Rectangle(0, 0, 4.5f, 4.5f);
        this.hitboxAtaque = new Rectangle();
        this.renderObj = new ObjetoRenderizavel();
        stateTime = 0f;
        carregarAnimacoes(assets);

        // Puxa o efeito sonoro já carregado na memória pelo AssetManager
        somMorte = assets.get("sons/Boss_Die.mp3", Sound.class);
    }

    private void carregarAnimacoes(AssetManager assets) {
        Texture idleSESheet = assets.get("boss/Idle/Idle_SE.png", Texture.class);
        idleSEAnimation = criarAnimacao(idleSESheet, 4, 0.15f, Animation.PlayMode.LOOP);

        Texture idleSWSheet = assets.get("boss/Idle/Idle_SW.png", Texture.class);
        idleSWAnimation = criarAnimacao(idleSWSheet, 4, 0.15f, Animation.PlayMode.LOOP);

        Texture walkSESheet = assets.get("boss/Walk/Walk_SE.png", Texture.class);
        walkSEAnimation = criarAnimacao(walkSESheet, 8, 0.15f, Animation.PlayMode.LOOP);

        Texture walkSWSheet = assets.get("boss/Walk/Walk_SW.png", Texture.class);
        walkSWAnimation = criarAnimacao(walkSWSheet, 8, 0.15f, Animation.PlayMode.LOOP);

        Texture attackSESheet = assets.get("boss/Attack1/Attack1_SE.png", Texture.class);
        attackSEAnimation = criarAnimacao(attackSESheet, 7, 0.12f, Animation.PlayMode.NORMAL);

        Texture attackSWSheet = assets.get("boss/Attack1/Attack1_SW.png", Texture.class);
        attackSWAnimation = criarAnimacao(attackSWSheet, 7, 0.12f, Animation.PlayMode.NORMAL);

        Texture attack2SESheet = assets.get("boss/Attack2/Attack2_SE.png", Texture.class);
        attack2SEAnimation = criarAnimacao(attack2SESheet, 8, 0.15f, Animation.PlayMode.NORMAL);

        Texture attack2SWSheet = assets.get("boss/Attack2/Attack2_SW.png", Texture.class);
        attack2SWAnimation = criarAnimacao(attack2SWSheet, 8, 0.15f, Animation.PlayMode.NORMAL);

        sheetProjetilSE = assets.get("boss/Proyectile/Proyectile_SE.png", Texture.class);
        sheetProjetilSW = assets.get("boss/Proyectile/Proyectile_SW.png", Texture.class);

        Texture deathSESheet = assets.get("boss/Death/Death_SE.png", Texture.class);
        deathSEAnimation = criarAnimacao(deathSESheet, 6, 0.15f, Animation.PlayMode.NORMAL);

        Texture deathSWSheet = assets.get("boss/Death/Death_SW.png", Texture.class);
        deathSWAnimation = criarAnimacao(deathSWSheet, 6, 0.15f, Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> criarAnimacao(Texture sheet, int quantidadeFrames, float duracaoFrame, Animation.PlayMode playMode) {
        int frameWidth = sheet.getWidth() / quantidadeFrames;
        int frameHeight = sheet.getHeight();

        TextureRegion[][] tmp = TextureRegion.split(sheet, frameWidth, frameHeight);
        TextureRegion[] frames = new TextureRegion[quantidadeFrames];

        for (int i = 0; i < quantidadeFrames; i++) {
            frames[i] = tmp[0][i];
        }

        Animation<TextureRegion> animacao = new Animation<TextureRegion>(duracaoFrame, frames);
        animacao.setPlayMode(playMode);
        return animacao;
    }

    public void update(float delta, Vector2 playerPosition, Rectangle playerHitbox, Array<Rectangle> hitboxesMapa, int larguraMapa, int alturaMapa) {
        if (isBlinking) {
            blinkTimer -= delta;
            if (blinkTimer <= 0) {
                isBlinking = false;
                renderObj.color = com.badlogic.gdx.graphics.Color.WHITE;
            } else {
                renderObj.color = com.badlogic.gdx.graphics.Color.RED;
            }
        }

        if (isDead) {
            stateTime += delta;
            return;
        }

        stateTime += delta;
        atualizarHitbox();

        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;
        if (timerCooldownRanged > 0f) timerCooldownRanged -= delta;

        float bossCenterX = hitbox.x + (hitbox.width / 2f);
        float bossCenterY = hitbox.y + (hitbox.height / 2f);

        float playerCenterX = playerHitbox.x + (playerHitbox.width / 2f);
        float playerCenterY = playerHitbox.y + (playerHitbox.height / 2f);

        float distance = Vector2.dst(bossCenterX, bossCenterY, playerCenterX, playerCenterY);

        String estadoAnterior = currentState;
        float margem = 1.5f;

        if (currentState.equals("ATTACK")) {
            if (animacaoAtaqueAtual().isAnimationFinished(attackStateTime)) {
                attackCooldownTimer = attackCooldown;
                precisaReiniciarAtaque = false;
                currentState = distance <= aggroRange ? "CHASE" : "IDLE";
            }
        }
        else if (currentState.equals("ATTACK2")) {
            if (animacaoAtaque2Atual().isAnimationFinished(attackStateTime)) {
                timerCooldownRanged = COOLDOWN_RANGED;
                currentState = distance <= aggroRange ? "CHASE" : "IDLE";
            }
        }
        else if (currentState.equals("CHASE")) {
            if (distance <= attackRange && attackCooldownTimer <= 0f) {
                currentState = "ATTACK";
            }
            else if (distance > attackRange && distance <= rangedAttackRange && timerCooldownRanged <= 0f) {
                currentState = "ATTACK2";
            }
            else if (distance > aggroRange + margem) {
                currentState = "IDLE";
            }
        }
        else {
            if (distance <= attackRange && attackCooldownTimer <= 0f) {
                currentState = "ATTACK";
            }
            else if (distance > attackRange && distance <= rangedAttackRange && timerCooldownRanged <= 0f) {
                currentState = "ATTACK2";
            }
            else if (distance <= aggroRange) {
                currentState = "CHASE";
            }
        }

        if (currentState.equals("ATTACK")) {
            atacar(delta, estadoAnterior, playerPosition, playerHitbox);
        }
        else if (currentState.equals("ATTACK2")) {
            atacarLonge(delta, estadoAnterior, playerPosition);
        }
        else if (currentState.equals("CHASE")) {
            perseguir(delta, playerPosition, playerHitbox, hitboxesMapa, larguraMapa, alturaMapa);
        }

        if (!currentState.equals(estadoAnterior)) {
            stateTime = 0f;
        }

        for (int i = teiasAtivas.size - 1; i >= 0; i--) {
            TeiaProjetil teia = teiasAtivas.get(i);
            teia.update(delta);
            if (teia.finalizada) {
                teiasAtivas.removeIndex(i);
            }
        }
    }

    private void perseguir(float delta, Vector2 playerPosition, Rectangle playerHitbox, Array<Rectangle> hitboxesMapa,
                           int larguraMapa, int alturaMapa) {

        timerTrocaModo -= delta;

        Vector2 direcaoDireta = new Vector2(playerPosition).sub(position).nor();

        float margemDecisao = 0.3f;
        Rectangle testeDecisaoX = calcularHitboxEm(position.x + direcaoDireta.x * margemDecisao, position.y);
        Rectangle testeDecisaoY = calcularHitboxEm(position.x, position.y + direcaoDireta.y * margemDecisao);

        boolean direcaoDiretaViavel = !colideComMapa(testeDecisaoX, hitboxesMapa) || !colideComMapa(testeDecisaoY, hitboxesMapa);

        if (timerTrocaModo <= 0f) {
            boolean novoModoPathfinding = !direcaoDiretaViavel;
            if (novoModoPathfinding != usandoPathfinding) {
                usandoPathfinding = novoModoPathfinding;
                timerTrocaModo = cooldownTrocaModo;
                if (!usandoPathfinding) caminhoAtual = null;
            }
        }

        if (!usandoPathfinding) {
            float deslocamentoX = direcaoDireta.x * speed * delta;
            float deslocamentoY = direcaoDireta.y * speed * delta;

            Rectangle testeX = calcularHitboxEm(position.x + deslocamentoX, position.y);
            Rectangle testeY = calcularHitboxEm(position.x, position.y + deslocamentoY);

            boolean bloqueadoX = colideComMapa(testeX, hitboxesMapa) || testeX.overlaps(playerHitbox);
            boolean bloqueadoY = colideComMapa(testeY, hitboxesMapa) || testeY.overlaps(playerHitbox);

            if (!bloqueadoX) position.x += deslocamentoX;
            if (!bloqueadoY) position.y += deslocamentoY;

            atualizarDirecaoVisual(direcaoDireta);
            atualizarHitbox();
            return;
        }

        timerRecalculoCaminho -= delta;
        if (caminhoAtual == null || timerRecalculoCaminho <= 0f) {
            caminhoAtual = Pathfinder.encontrarCaminho(position, playerPosition, hitboxesMapa, larguraMapa, alturaMapa, 2);
            indiceCaminho = 0;
            timerRecalculoCaminho = intervaloRecalculo;
        }

        if (caminhoAtual == null || caminhoAtual.size == 0) return;
        if (indiceCaminho >= caminhoAtual.size) { caminhoAtual = null; return; }

        Vector2 alvoAtual = caminhoAtual.get(indiceCaminho);
        Vector2 direcaoRota = new Vector2(alvoAtual).sub(position);

        if (direcaoRota.len() < 0.2f) {
            indiceCaminho++;
            return;
        }

        direcaoRota.nor();
        float dx = direcaoRota.x * speed * delta;
        float dy = direcaoRota.y * speed * delta;

        Rectangle t1 = calcularHitboxEm(position.x + dx, position.y);
        if (!colideComMapa(t1, hitboxesMapa) && !t1.overlaps(playerHitbox)) position.x += dx;

        Rectangle t2 = calcularHitboxEm(position.x, position.y + dy);
        if (!colideComMapa(t2, hitboxesMapa) && !t2.overlaps(playerHitbox)) position.y += dy;

        atualizarDirecaoVisual(direcaoRota);
        atualizarHitbox();
    }

    private void atacar(float delta, String estadoAnterior, Vector2 playerPosition, Rectangle playerHitbox) {
        boolean primeiraVezEntrandoEmAttack = !estadoAnterior.equals("ATTACK");

        if (primeiraVezEntrandoEmAttack || precisaReiniciarAtaque) {
            precisaReiniciarAtaque = false;
            attackStateTime = 0f;
            danoAplicado = false;

            float bossCenterX = hitbox.x + (hitbox.width / 2f);
            float bossCenterY = hitbox.y + (hitbox.height / 2f);

            float playerCenterX = playerHitbox.x + (playerHitbox.width / 2f);
            float playerCenterY = playerHitbox.y + (playerHitbox.height / 2f);

            Vector2 direcaoParaPlayer = new Vector2(playerCenterX - bossCenterX, playerCenterY - bossCenterY);

            direcaoAtual = direcaoParaPlayer.x >= 0 ? "SE" : "SW";

            Vector2 direcaoNormalizada = direcaoParaPlayer.nor();
            direcaoVetorAtaque.set(MathUtils.round(direcaoNormalizada.x), MathUtils.round(direcaoNormalizada.y));

            if (direcaoVetorAtaque.x == 0 && direcaoVetorAtaque.y == 0) {
                direcaoVetorAtaque.set(1, 0);
            }

            alvoAtaqueTravar.set(playerPosition);
            calcularTilesTelegraph();
        }

        attackStateTime += delta;

        Animation<TextureRegion> anim = animacaoAtaqueAtual();
        float progresso = anim.getAnimationDuration() > 0f
            ? Math.min(attackStateTime / anim.getAnimationDuration(), 1f)
            : 1f;

        boolean dentroDaJanelaDeImpacto = progresso >= PROGRESSO_IMPACTO_INICIO && progresso <= PROGRESSO_IMPACTO_FIM;

        if (dentroDaJanelaDeImpacto) {
            atualizarHitboxAtaque();
        } else {
            hitboxAtaque.set(0, 0, 0, 0);
        }

        if (dentroDaJanelaDeImpacto && !danoAplicado) {
            danoAplicado = true;
        }
    }

    private Animation<TextureRegion> animacaoAtaqueAtual() {
        return direcaoAtual.equals("SE") ? attackSEAnimation : attackSWAnimation;
    }

    private void calcularTilesTelegraph() {
        tilesAtaqueTelegraph.clear();
        float offsetDistancia = 3.5f;

        float bossCenterX = hitbox.x + (hitbox.width / 2f);
        float bossCenterY = hitbox.y + (hitbox.height / 2f);

        int centroX = MathUtils.floor(bossCenterX + (direcaoVetorAtaque.x * offsetDistancia));
        int centroY = MathUtils.floor(bossCenterY + (direcaoVetorAtaque.y * offsetDistancia));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                tilesAtaqueTelegraph.add(new Vector2(centroX + dx, centroY + dy));
            }
        }
    }

    private void atualizarHitboxAtaque() {
        if (tilesAtaqueTelegraph.size == 0) {
            hitboxAtaque.set(0, 0, 0, 0);
            return;
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (Vector2 tile : tilesAtaqueTelegraph) {
            minX = Math.min(minX, tile.x);
            minY = Math.min(minY, tile.y);
            maxX = Math.max(maxX, tile.x + 1f);
            maxY = Math.max(maxY, tile.y + 1f);
        }

        hitboxAtaque.set(minX, minY, maxX - minX, maxY - minY);
    }

    public boolean isAtacando() {
        return currentState.equals("ATTACK");
    }

    public boolean isGolpeAtivo() {
        if (!currentState.equals("ATTACK")) return false;
        Animation<TextureRegion> anim = animacaoAtaqueAtual();
        float progresso = anim.getAnimationDuration() > 0f
            ? Math.min(attackStateTime / anim.getAnimationDuration(), 1f)
            : 1f;
        return progresso >= PROGRESSO_IMPACTO_INICIO && progresso <= PROGRESSO_IMPACTO_FIM;
    }

    public Array<Vector2> getTilesAtaqueTelegraph() {
        return tilesAtaqueTelegraph;
    }

    private void atualizarDirecaoVisual(Vector2 direcao) {
        timerTrocaDirecao -= Gdx.graphics.getDeltaTime();

        String novaDirecao = direcao.x >= 0 ? "SE" : "SW";
        if (timerTrocaDirecao <= 0f && !novaDirecao.equals(direcaoAtual)) {
            direcaoAtual = novaDirecao;
            timerTrocaDirecao = cooldownTrocaDirecao;
        }
    }

    private boolean colideComMapa(Rectangle hitboxTeste, Array<Rectangle> hitboxesMapa) {
        for (Rectangle parede : hitboxesMapa) {
            if (hitboxTeste.overlaps(parede)) {
                return true;
            }
        }
        return false;
    }

    private Rectangle calcularHitboxEm(float x, float y) {
        float offsetX = -2f;
        float offsetY = -2f;
        float largura = 4.5f;
        float altura = 4.5f;
        return new Rectangle(x + offsetX, y + offsetY, largura, altura);
    }

    public TextureRegion getCurrentFrame() {
        switch (currentState) {
            case "CHASE":
                return direcaoAtual.equals("SE")
                    ? walkSEAnimation.getKeyFrame(stateTime)
                    : walkSWAnimation.getKeyFrame(stateTime);

            case "ATTACK":
                return direcaoAtual.equals("SE")
                    ? attackSEAnimation.getKeyFrame(attackStateTime)
                    : attackSWAnimation.getKeyFrame(attackStateTime);

            case "ATTACK2":
                return direcaoAtual.equals("SE")
                    ? attack2SEAnimation.getKeyFrame(attackStateTime)
                    : attack2SWAnimation.getKeyFrame(attackStateTime);

            case "DEATH":
                return direcaoAtual.equals("SE")
                    ? deathSEAnimation.getKeyFrame(stateTime, false)
                    : deathSWAnimation.getKeyFrame(stateTime, false);

            case "IDLE":
            default:
                return direcaoAtual.equals("SE")
                    ? idleSEAnimation.getKeyFrame(stateTime)
                    : idleSWAnimation.getKeyFrame(stateTime);
        }
    }

    public float getScreenX() {
        return (position.x - position.y) * (TILE_WIDTH / 2f);
    }

    public float getScreenY() {
        return (position.x + position.y) * (TILE_HEIGHT / 2f);
    }

    private void atualizarHitbox() {
        Rectangle r = calcularHitboxEm(position.x, position.y);
        hitbox.set(r.x, r.y, r.width, r.height);
    }

    private Animation<TextureRegion> animacaoAtaque2Atual() {
        return direcaoAtual.equals("SE") ? attack2SEAnimation : attack2SWAnimation;
    }

    private void atacarLonge(float delta, String estadoAnterior, Vector2 playerPosition) {
        boolean primeiraVez = !estadoAnterior.equals("ATTACK2");

        if (primeiraVez) {
            attackStateTime = 0f;
            projetilDisparado = false;

            Vector2 direcaoParaPlayer = new Vector2(playerPosition).sub(position);
            direcaoAtual = direcaoParaPlayer.x >= 0 ? "SE" : "SW";
            alvoAtaqueTravar.set(playerPosition);
        }

        attackStateTime += delta;
        Animation<TextureRegion> anim = animacaoAtaque2Atual();
        float progresso = anim.getAnimationDuration() > 0f ? Math.min(attackStateTime / anim.getAnimationDuration(), 1f) : 1f;

        if (progresso >= 0.5f && !projetilDisparado) {
            Texture sheetCerta = direcaoAtual.equals("SE") ? sheetProjetilSE : sheetProjetilSW;
            TeiaProjetil novaTeia = new TeiaProjetil(this.position, alvoAtaqueTravar, direcaoAtual, sheetCerta);
            teiasAtivas.add(novaTeia);
            projetilDisparado = true;
        }
    }

    public void tomarDano(int dano) {
        if (isDead) return;

        vida -= dano;
        isBlinking = true;
        blinkTimer = 0.2f;

        if (vida <= 0) {
            vida = 0;
            isDead = true;
            currentState = "DEATH";
            stateTime = 0f;

            // --- TOCA O SOM DA MORTE ---
            if (somMorte != null) {
                somMorte.play(1.0f); // 1.0f é o volume (100%)
            }
        }
    }
}
