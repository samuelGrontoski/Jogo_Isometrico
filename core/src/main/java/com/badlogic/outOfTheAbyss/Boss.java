package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Boss {
    public Vector2 position;
    public float speed = 7f;
    public float aggroRange = 25f;
    public float attackRange = 6f;
    public String currentState = "IDLE";

    public Rectangle hitbox;
    public Rectangle hitboxAtaque;

    private Animation<TextureRegion> idleSEAnimation;
    private Animation<TextureRegion> idleSWAnimation;
    private Animation<TextureRegion> walkSEAnimation;
    private Animation<TextureRegion> walkSWAnimation;
    private Animation<TextureRegion> attackSEAnimation;
    private Animation<TextureRegion> attackSWAnimation;
    private float stateTime;
    private String direcaoAtual = "SE";

    private Array<Vector2> caminhoAtual;
    private int indiceCaminho = 0;
    private float timerRecalculoCaminho = 0f;
    private final float intervaloRecalculo = 0.5f;

    private boolean usandoPathfinding = false;
    private float timerTrocaModo = 0f;
    private final float cooldownTrocaModo = 0.4f; // tempo mínimo antes de poder alternar de novo

    private float timerTrocaDirecao = 0f;
    private final float cooldownTrocaDirecao = 0.15f; // evita a sprite "tremer" de direção

    // --- SISTEMA DE ATAQUE ---
    private float attackStateTime = 0f;
    private boolean danoAplicado = false;
    private boolean precisaReiniciarAtaque = false;
    // Fração da duração da animação em que o "golpe" realmente acontece
    // (a hitbox de dano só fica ativa perto desse ponto). Ajuste conforme o
    // frame de impacto real da sua sprite sheet.
    private final float PROGRESSO_IMPACTO_INICIO = 0.55f;
    private final float PROGRESSO_IMPACTO_FIM = 0.75f;

    private final Array<Vector2> tilesAtaqueTelegraph = new Array<>();
    private final Vector2 direcaoVetorAtaque = new Vector2(1, 0);
    private final Vector2 alvoAtaqueTravar = new Vector2();
    private float attackCooldownTimer = 0f;
    public float attackCooldown = 0.5f;

    // Ataque de Teia
    public float rangedAttackRange = 12f; // Distância máxima para atirar a teia
    private float timerCooldownRanged = 0f;
    private final float COOLDOWN_RANGED = 3.0f; // Demora mais pra atirar teia de novo

    private Animation<TextureRegion> attack2SEAnimation;
    private Animation<TextureRegion> attack2SWAnimation;

    private Texture sheetProjetilSE;
    private Texture sheetProjetilSW;

    public Array<TeiaProjetil> teiasAtivas = new Array<>();
    private boolean projetilDisparado = false; // Garante que só atire 1 vez por animação

    private static final float TILE_WIDTH = 32f;
    private static final float TILE_HEIGHT = 16f;

    public Boss(Vector2 spawn, AssetManager assets) {
        position = spawn;
        this.hitbox = new Rectangle(0, 0, 4.5f, 4.5f);
        this.hitboxAtaque = new Rectangle();
        stateTime = 0f;
        carregarAnimacoes(assets);
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

        // Ajuste "7" para o número real de frames do seu sheet de ataque
        Texture attackSESheet = assets.get("boss/Attack1/Attack1_SE.png", Texture.class);
        attackSEAnimation = criarAnimacao(attackSESheet, 7, 0.12f, Animation.PlayMode.NORMAL);

        Texture attackSWSheet = assets.get("boss/Attack1/Attack1_SW.png", Texture.class);
        attackSWAnimation = criarAnimacao(attackSWSheet, 7, 0.12f, Animation.PlayMode.NORMAL);

        // Carrega o Ataque 2 (ajuste o '7' para a quantidade certa de frames da sua imagem)
        Texture attack2SESheet = assets.get("boss/Attack2/Attack2_SE.png", Texture.class);
        attack2SEAnimation = criarAnimacao(attack2SESheet, 8, 0.15f, Animation.PlayMode.NORMAL);

        Texture attack2SWSheet = assets.get("boss/Attack2/Attack2_SW.png", Texture.class);
        attack2SWAnimation = criarAnimacao(attack2SWSheet, 8, 0.15f, Animation.PlayMode.NORMAL);

        // Carrega os projéteis (você precisa garantir que esses caminhos estejam certos)
        sheetProjetilSE = assets.get("boss/Proyectile/Proyectile_SE.png", Texture.class);
        sheetProjetilSW = assets.get("boss/Proyectile/Proyectile_SW.png", Texture.class);
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
        stateTime += delta;
        atualizarHitbox();

        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;
        if (timerCooldownRanged > 0f) timerCooldownRanged -= delta;

        // --- CORREÇÃO DA DISTÂNCIA ---
        // Calcula o ponto central exato de ambas as hitboxes
        float bossCenterX = hitbox.x + (hitbox.width / 2f);
        float bossCenterY = hitbox.y + (hitbox.height / 2f);

        float playerCenterX = playerHitbox.x + (playerHitbox.width / 2f);
        float playerCenterY = playerHitbox.y + (playerHitbox.height / 2f);

        // Calcula a distância real baseada nos centros
        float distance = Vector2.dst(bossCenterX, bossCenterY, playerCenterX, playerCenterY);

        String estadoAnterior = currentState;
        float margem = 1.5f;

        // --- MÁQUINA DE ESTADOS ---
        if (currentState.equals("ATTACK")) {
            // Se a animação do ataque de perto terminou
            if (animacaoAtaqueAtual().isAnimationFinished(attackStateTime)) {
                attackCooldownTimer = attackCooldown;
                precisaReiniciarAtaque = false;
                currentState = distance <= aggroRange ? "CHASE" : "IDLE";
            }
        }
        else if (currentState.equals("ATTACK2")) {
            // Se a animação do ataque de longe (teia) terminou
            if (animacaoAtaque2Atual().isAnimationFinished(attackStateTime)) {
                timerCooldownRanged = COOLDOWN_RANGED; // Inicia o cooldown da teia!
                currentState = distance <= aggroRange ? "CHASE" : "IDLE";
            }
        }
        else if (currentState.equals("CHASE")) {
            if (distance <= attackRange && attackCooldownTimer <= 0f) {
                currentState = "ATTACK"; // Player está colado, ataca de perto
            }
            else if (distance > attackRange && distance <= rangedAttackRange && timerCooldownRanged <= 0f) {
                currentState = "ATTACK2"; // Player está longe, mas no alcance da teia
            }
            else if (distance > aggroRange + margem) {
                currentState = "IDLE";
            }
        }
        else { // IDLE
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

        // --- EXECUTA A AÇÃO DO ESTADO ATUAL ---
        if (currentState.equals("ATTACK")) {
            atacar(delta, estadoAnterior, playerPosition, playerHitbox);
        }
        else if (currentState.equals("ATTACK2")) {
            atacarLonge(delta, estadoAnterior, playerPosition); // Chama a lógica de atirar a teia
        }
        else if (currentState.equals("CHASE")) {
            perseguir(delta, playerPosition, playerHitbox, hitboxesMapa, larguraMapa, alturaMapa);
        }

        if (!currentState.equals(estadoAnterior)) {
            stateTime = 0f;
        }

        // --- ATUALIZA AS TEIAS NO MUNDO ---
        // Fazemos um for de trás pra frente (i--) para poder remover as teias antigas sem bugar a lista
        for (int i = teiasAtivas.size - 1; i >= 0; i--) {
            TeiaProjetil teia = teiasAtivas.get(i);
            teia.update(delta);

            // Se a teia expirou (sumiu do chão), remove da lista
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

            // ADICIONE A CHECAGEM COM O PLAYER AQUI:
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
        // ADICIONE A CHECAGEM COM O PLAYER AQUI TAMBÉM:
        if (!colideComMapa(t1, hitboxesMapa) && !t1.overlaps(playerHitbox)) position.x += dx;

        Rectangle t2 = calcularHitboxEm(position.x, position.y + dy);
        // E AQUI:
        if (!colideComMapa(t2, hitboxesMapa) && !t2.overlaps(playerHitbox)) position.y += dy;

        atualizarDirecaoVisual(direcaoRota);
        atualizarHitbox();
    }

    // --- LÓGICA DE ATAQUE ---

    // Alteramos a assinatura para receber o playerHitbox
    private void atacar(float delta, String estadoAnterior, Vector2 playerPosition, Rectangle playerHitbox) {
        boolean primeiraVezEntrandoEmAttack = !estadoAnterior.equals("ATTACK");

        if (primeiraVezEntrandoEmAttack || precisaReiniciarAtaque) {
            precisaReiniciarAtaque = false;
            attackStateTime = 0f;
            danoAplicado = false;

            // 1. Calcula o centro exato das duas entidades
            float bossCenterX = hitbox.x + (hitbox.width / 2f);
            float bossCenterY = hitbox.y + (hitbox.height / 2f);

            float playerCenterX = playerHitbox.x + (playerHitbox.width / 2f);
            float playerCenterY = playerHitbox.y + (playerHitbox.height / 2f);

            // 2. Calcula a direção usando os centros (Sem distorção da sprite)
            Vector2 direcaoParaPlayer = new Vector2(playerCenterX - bossCenterX, playerCenterY - bossCenterY);

            // Define a direção visual (sprite SE ou SW)
            direcaoAtual = direcaoParaPlayer.x >= 0 ? "SE" : "SW";

            // 3. Trava o vetor de ataque nos 8 eixos
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

    /**
     * Calcula os tiles da área de ataque considerando as 8 direções possíveis
     * (N, NE, E, SE, S, SW, W, NW), sempre alinhados ao grid do mapa.
     *
     * A direção real até o player (direcaoVetorAtaque) é "arredondada" pro
     * múltiplo de 45° mais próximo, virando um vetor com componentes exatas
     * -1, 0 ou 1. Isso garante que:
     * - Direções diagonais (NE/NW/SE/SW) gerem um quadrado 3x3 deslocado
     *   nos dois eixos, sem rotação (sem tiles "dispersos").
     * - Direções retas (N/S/L/O) gerem um bloco 3x3 esticado no eixo
     *   dominante, com a largura no outro eixo (que já é naturalmente
     *   alinhado ao grid, sem precisar de rotação).
     */
    private void calcularTilesTelegraph() {
        tilesAtaqueTelegraph.clear();

        float offsetDistancia = 3.5f;

        // Resgata o centro da hitbox amarela do boss
        float bossCenterX = hitbox.x + (hitbox.width / 2f);
        float bossCenterY = hitbox.y + (hitbox.height / 2f);

        // O centro do grid 3x3 agora nasce do centro real do Boss
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

    /** true enquanto a animação de ataque estiver tocando (pra desenhar o telegraph). */
    public boolean isAtacando() {
        return currentState.equals("ATTACK");
    }

    /** true só durante a janela de impacto real (hitbox de dano ativa). */
    public boolean isGolpeAtivo() {
        if (!currentState.equals("ATTACK")) return false;
        Animation<TextureRegion> anim = animacaoAtaqueAtual();
        float progresso = anim.getAnimationDuration() > 0f
            ? Math.min(attackStateTime / anim.getAnimationDuration(), 1f)
            : 1f;
        return progresso >= PROGRESSO_IMPACTO_INICIO && progresso <= PROGRESSO_IMPACTO_FIM;
    }

    /** Tiles (em coordenadas de mundo) que devem ser destacados em vermelho durante o telegraph. */
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

            case "ATTACK2": // ADICIONE ESTE CASE
                return direcaoAtual.equals("SE")
                    ? attack2SEAnimation.getKeyFrame(attackStateTime)
                    : attack2SWAnimation.getKeyFrame(attackStateTime);

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

            // Encara o player
            Vector2 direcaoParaPlayer = new Vector2(playerPosition).sub(position);
            direcaoAtual = direcaoParaPlayer.x >= 0 ? "SE" : "SW";
            alvoAtaqueTravar.set(playerPosition); // Trava a mira!
        }

        attackStateTime += delta;
        Animation<TextureRegion> anim = animacaoAtaque2Atual();
        float progresso = anim.getAnimationDuration() > 0f ? Math.min(attackStateTime / anim.getAnimationDuration(), 1f) : 1f;

        // Dispara a teia por volta da metade da animação (ajuste esse 0.5f conforme o visual)
        if (progresso >= 0.5f && !projetilDisparado) {
            Texture sheetCerta = direcaoAtual.equals("SE") ? sheetProjetilSE : sheetProjetilSW;

            // Cria a teia saindo da posição do boss e indo até a mira travada
            TeiaProjetil novaTeia = new TeiaProjetil(this.position, alvoAtaqueTravar, direcaoAtual, sheetCerta);
            teiasAtivas.add(novaTeia);

            projetilDisparado = true;
        }
    }
}
