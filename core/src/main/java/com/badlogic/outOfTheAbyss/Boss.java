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
    public float aggroRange = 30f;
    public float attackRange = 5f;
    public String currentState = "IDLE";

    public Rectangle hitbox;
    public Rectangle hitboxAtaque;

    private Animation<TextureRegion> idleSEAnimation;
    private Animation<TextureRegion> idleSWAnimation;
    private Animation<TextureRegion> walkSEAnimation;
    private Animation<TextureRegion> walkSWAnimation;
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
        Texture idleSESheet = assets.get("boss/Idle/Idle_SE.png", Texture.class); // ou Idle_SE.png se você renomeou
        idleSEAnimation = criarAnimacao(idleSESheet, 4, 0.15f, Animation.PlayMode.LOOP);

        Texture idleSWSheet = assets.get("boss/Idle/Idle_SW.png", Texture.class);
        idleSWAnimation = criarAnimacao(idleSWSheet, 4, 0.15f, Animation.PlayMode.LOOP);

        Texture walkSESheet = assets.get("boss/Walk/Walk_SE.png", Texture.class);
        walkSEAnimation = criarAnimacao(walkSESheet, 8, 0.15f, Animation.PlayMode.LOOP);

        Texture walkSWSheet = assets.get("boss/Walk/Walk_SW.png", Texture.class);
        walkSWAnimation = criarAnimacao(walkSWSheet, 8, 0.15f, Animation.PlayMode.LOOP);
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

    public void update(float delta, Vector2 playerPosition, Array<Rectangle> hitboxesMapa,
                       int larguraMapa, int alturaMapa) {
        stateTime += delta;
        atualizarHitbox();

        float distance = position.dst(playerPosition);
        String estadoAnterior = currentState;

        float margem = 1.5f;

        if (currentState.equals("ATTACK")) {
            if (distance > attackRange + margem) {
                currentState = distance <= aggroRange ? "CHASE" : "IDLE";
            }
        } else if (currentState.equals("CHASE")) {
            if (distance <= attackRange) {
                currentState = "ATTACK";
            } else if (distance > aggroRange + margem) {
                currentState = "IDLE";
            }
        } else {
            if (distance <= attackRange) {
                currentState = "ATTACK";
            } else if (distance <= aggroRange) {
                currentState = "CHASE";
            }
        }

        if (currentState.equals("ATTACK")) {
            atacar();
        } else if (currentState.equals("CHASE")) {
            perseguir(delta, playerPosition, hitboxesMapa, larguraMapa, alturaMapa);
        }

        if (!currentState.equals(estadoAnterior)) {
            stateTime = 0f;
        }
    }

    private void perseguir(float delta, Vector2 playerPosition, Array<Rectangle> hitboxesMapa,
                           int larguraMapa, int alturaMapa) {

        timerTrocaModo -= delta;

        Vector2 direcaoDireta = new Vector2(playerPosition).sub(position).nor();

        // Testa um passo um pouco maior que o real, só pra decidir o MODO (direto vs pathfinding),
        // criando uma margem de segurança que evita alternância por 1 pixel de diferença.
        float margemDecisao = 0.3f;
        Rectangle testeDecisaoX = calcularHitboxEm(position.x + direcaoDireta.x * margemDecisao, position.y);
        Rectangle testeDecisaoY = calcularHitboxEm(position.x, position.y + direcaoDireta.y * margemDecisao);

        boolean direçãoDiretaViavel = !colideComMapa(testeDecisaoX, hitboxesMapa) || !colideComMapa(testeDecisaoY, hitboxesMapa);

        // Só permite trocar de modo (direto <-> pathfinding) se o cooldown já passou
        if (timerTrocaModo <= 0f) {
            boolean novoModoPathfinding = !direçãoDiretaViavel;
            if (novoModoPathfinding != usandoPathfinding) {
                usandoPathfinding = novoModoPathfinding;
                timerTrocaModo = cooldownTrocaModo;
                if (!usandoPathfinding) caminhoAtual = null; // limpa rota antiga ao voltar pro modo direto
            }
        }

        if (!usandoPathfinding) {
            float deslocamentoX = direcaoDireta.x * speed * delta;
            float deslocamentoY = direcaoDireta.y * speed * delta;

            Rectangle testeX = calcularHitboxEm(position.x + deslocamentoX, position.y);
            Rectangle testeY = calcularHitboxEm(position.x, position.y + deslocamentoY);

            boolean bloqueadoX = colideComMapa(testeX, hitboxesMapa);
            boolean bloqueadoY = colideComMapa(testeY, hitboxesMapa);

            if (!bloqueadoX) position.x += deslocamentoX;
            if (!bloqueadoY) position.y += deslocamentoY;

            atualizarDirecaoVisual(direcaoDireta);
            atualizarHitbox();
            return;
        }

        // --- modo pathfinding ---
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
        if (!colideComMapa(t1, hitboxesMapa)) position.x += dx;

        Rectangle t2 = calcularHitboxEm(position.x, position.y + dy);
        if (!colideComMapa(t2, hitboxesMapa)) position.y += dy;

        atualizarDirecaoVisual(direcaoRota);
        atualizarHitbox();
    }

    private void atualizarDirecaoVisual(Vector2 direcao) {
        timerTrocaDirecao -= Gdx.graphics.getDeltaTime(); // ou passe "delta" como parâmetro se preferir

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
        float offsetX = -2f; // use os mesmos valores que você já calibrou
        float offsetY = -2f;
        float largura = 4.5f;
        float altura = 4.5f;
        return new Rectangle(x + offsetX, y + offsetY, largura, altura);
    }

    private void atacar() {
        // Lógica de ataque
    }

    public TextureRegion getCurrentFrame() {
        switch (currentState) {
            case "CHASE":
                return direcaoAtual.equals("SE")
                    ? walkSEAnimation.getKeyFrame(stateTime)
                    : walkSWAnimation.getKeyFrame(stateTime);

            case "ATTACK":
                return direcaoAtual.equals("SE")
                    ? idleSEAnimation.getKeyFrame(stateTime)
                    : idleSWAnimation.getKeyFrame(stateTime);

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
}
