package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.drop.GameScreen.ObjetoRenderizavel;

public class Morcego {
    // --- Física e Mundo ---
    public Vector2 posicaoMundo;
    public Rectangle hitboxColisao;

    // --- Animação e Visual ---
    Texture sheet;
    Animation<TextureRegion> animacaoIdle;
    float localStateTime;
    final int TOTAL_FRAMES = 4;

    final float ELEVACAO_VISUAL = 24f;

    public ObjetoRenderizavel renderObj;

    public Morcego(Vector2 posicaoInicial) {
        this.posicaoMundo = posicaoInicial;
        this.hitboxColisao = new Rectangle(0, 0, 1f, 1f);
        atualizarHitboxLógica();

        this.renderObj = new ObjetoRenderizavel();
        this.localStateTime = 0f;

        carregarAnimacoes();
    }

    private void carregarAnimacoes() {
        // Carrega a sua nova Sprite Sheet linear (4x1)
        sheet = new Texture("morcego_idle.png");

        // Opcional: Se cada frame tiver 64x64, você pode manter 64, 64.
        // Mas usar a matemática abaixo garante que sempre vai funcionar,
        // mesmo que você troque a arte por uma de tamanho diferente no futuro!
        int larguraFrame = sheet.getWidth() / TOTAL_FRAMES;
        int alturaFrame = sheet.getHeight();

        // Corta a imagem (Isso devolve uma matriz com 1 linha e 4 colunas)
        TextureRegion[][] tmpStrip = TextureRegion.split(sheet, larguraFrame, alturaFrame);

        // Prepara o array final de 4 posições
        TextureRegion[] frames = new TextureRegion[TOTAL_FRAMES];

        // Copia a linha inteira de uma vez só, direto para o array final!
        // (De onde: tmpStrip[0], Posição inicial: 0, Para onde: frames, Posição inicial: 0, Quantidade: TOTAL_FRAMES)
        System.arraycopy(tmpStrip[0], 0, frames, 0, TOTAL_FRAMES);

        // Cria a animação e configura o loop (0.1s por frame)
        animacaoIdle = new Animation<>(0.1f, frames);
        animacaoIdle.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta) {
        // Toca a animação
        localStateTime += delta;

        // Pega o frame atual
        renderObj.textura = animacaoIdle.getKeyFrame(localStateTime, true);

        // Por enquanto, o morcego está parado, então a hitbox não se move.
        // Se no futuro ele voar, nós chamaríamos atualizarHitboxLógica() aqui.
    }
}
