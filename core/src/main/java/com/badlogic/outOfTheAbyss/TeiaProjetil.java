package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;

public class TeiaProjetil implements  Poolable{
    public Vector2 posicaoMundo;
    private Vector2 alvoMundo;
    private Vector2 direcao;
    private float velocidade = 12f;

    public boolean voando = true;
    public boolean finalizada = false;

    public Rectangle hitbox;

    private Vector2 posicaoInicial;
    public float distanciaMaxima = 15f;
    private float tempoNoChao = 0f;
    private final float TEMPO_DURACAO_TEIA = 4.0f;

    // Animações
    private Animation<TextureRegion> animacaoVoando;
    private TextureRegion teiaNoChao;
    public float stateTime = 0f;
    private String direcaoSprite;

    public TeiaProjetil() {
        this.posicaoMundo = new Vector2();
        this.posicaoInicial = new Vector2();
        this.alvoMundo = new Vector2();
        this.direcao = new Vector2();
        this.hitbox = new Rectangle();
    }

    public void init(Vector2 spawnPos, Vector2 alvoPos, String direcaoBoss, Texture sheetTeia) {
        this.posicaoMundo.set(spawnPos);
        this.posicaoInicial.set(spawnPos);
        this.alvoMundo.set(alvoPos);
        this.direcaoSprite = direcaoBoss;

        this.direcao.set(alvoMundo).sub(posicaoMundo).nor();
        this.hitbox.set(posicaoMundo.x, posicaoMundo.y, 1f, 1f);

        TextureRegion[][] tmp = TextureRegion.split(sheetTeia, sheetTeia.getWidth() / 4, sheetTeia.getHeight());
        TextureRegion[] framesVoo = { tmp[0][0], tmp[0][1] };
        animacaoVoando = new Animation<>(0.1f, framesVoo);
        animacaoVoando.setPlayMode(Animation.PlayMode.LOOP);
        teiaNoChao = tmp[0][3];
    }

    public void update(float delta, Array<Rectangle> hitboxesMapa) {
        stateTime += delta;

        if (voando) {
            // Move o projétil
            posicaoMundo.x += direcao.x * velocidade * delta;
            posicaoMundo.y += direcao.y * velocidade * delta;

            hitbox.setPosition(posicaoMundo.x, posicaoMundo.y);

            if (hitboxesMapa != null) {
                for (Rectangle parede : hitboxesMapa) {
                    if (this.hitbox.overlaps(parede)) {
                        voando = false; // Bateu na parede, cai no chão instantaneamente
                        stateTime = 0f;
                        break; // Para a verificação assim que bater na primeira parede
                    }
                }
            }

            // --- LÓGICA DE DISTÂNCIA MÁXIMA ---
            // Só verifica a distância se a teia AINDA estiver voando (ou seja, se não bateu na parede acima)
            if (voando && posicaoMundo.dst(posicaoInicial) >= distanciaMaxima) {
                voando = false; // Acabou o fôlego, caiu no chão!
                stateTime = 0f;
            }

            // Verifica se chegou perto do alvo
            if (posicaoMundo.dst(posicaoInicial) >= distanciaMaxima) {
                voando = false;
                stateTime = 0f;
            }
        } else {
            // Teia está no chão agindo como armadilha
            this.hitbox = new Rectangle(posicaoMundo.x, posicaoMundo.y, 3f, 3f);
            tempoNoChao += delta;
            if (tempoNoChao >= TEMPO_DURACAO_TEIA) {
                finalizada = true;
            }
        }
    }

    public TextureRegion getCurrentFrame() {
        if (voando) {
            return animacaoVoando.getKeyFrame(stateTime);
        } else {
            return teiaNoChao;
        }
    }

    @Override
    public void reset() {
        this.voando = true;
        this.finalizada = false;
        this.tempoNoChao = 0f;
        this.stateTime = 0f;
    }
}
