package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class TeiaProjetil {
    public Vector2 posicaoMundo;
    private Vector2 alvoMundo;
    private Vector2 direcao;
    private float velocidade = 12f;

    public boolean voando = true;
    public boolean finalizada = false; // true quando sumir totalmente

    public Rectangle hitbox;

    private Vector2 posicaoInicial;
    public float distanciaMaxima = 15f; // Quantos blocos a teia voa antes de cair (ajuste como quiser)
    private float tempoNoChao = 0f;
    private final float TEMPO_DURACAO_TEIA = 4.0f; // Quanto tempo a teia fica no chão

    // Animações
    private Animation<TextureRegion> animacaoVoando;
    private TextureRegion teiaNoChao;
    public float stateTime = 0f;
    private String direcaoSprite;

    public TeiaProjetil(Vector2 spawnPos, Vector2 alvoPos, String direcaoBoss, Texture sheetTeia) {
        this.posicaoMundo = new Vector2(spawnPos);
        this.posicaoInicial = new Vector2(spawnPos);

        this.alvoMundo = new Vector2(alvoPos);
        this.direcaoSprite = direcaoBoss;

        this.direcao = new Vector2(alvoMundo).sub(posicaoMundo).nor();
        this.hitbox = new Rectangle(posicaoMundo.x, posicaoMundo.y, 3f, 3f);

        // Recortando o SpriteSheet do projétil (4 frames)
        TextureRegion[][] tmp = TextureRegion.split(sheetTeia, sheetTeia.getWidth() / 4, sheetTeia.getHeight());

        // Vamos usar o primeiro frame para a teia voando (com uma leve animação se quiser, mas aqui fixamos o 1º)
        TextureRegion[] framesVoo = { tmp[0][0], tmp[0][1] };
        animacaoVoando = new Animation<>(0.1f, framesVoo);
        animacaoVoando.setPlayMode(Animation.PlayMode.LOOP);

        // O último frame é a teia aberta no chão
        teiaNoChao = tmp[0][3];
    }

    public void update(float delta) {
        stateTime += delta;

        if (voando) {
            // Move o projétil
            posicaoMundo.x += direcao.x * velocidade * delta;
            posicaoMundo.y += direcao.y * velocidade * delta;

            hitbox.setPosition(posicaoMundo.x, posicaoMundo.y);

            // Verifica se chegou perto do alvo
            if (posicaoMundo.dst(posicaoInicial) >= distanciaMaxima) {
                voando = false; // Acabou o fôlego, caiu no chão!
                stateTime = 0f;
            }
        } else {
            // Teia está no chão agindo como armadilha
            tempoNoChao += delta;
            if (tempoNoChao >= TEMPO_DURACAO_TEIA) {
                finalizada = true; // Tempo acabou, avisa pra remover
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
}
