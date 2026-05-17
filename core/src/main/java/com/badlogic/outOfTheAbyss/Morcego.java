package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;

public class Morcego implements Poolable {
    public boolean isAtivo = true;
    public float timerMorto = 0f;
    public final float tempo_respawn = 3.0f;

    // Status simples
    public int vida = 3;
    public final int vida_maxima = 3;

    public Vector2 posicaoMundo;
    public Rectangle hitboxColisao;
    public float velocidade = 3.5f;

    private Animation<TextureRegion> animacaoIdle;
    float localStateTime;
    final int quantidade_frames = 4;
    public final float elevacao_visual = 24f;

    public ObjetoRenderizavel renderObj;

    public Morcego() {
        this.posicaoMundo = new Vector2();
        this.hitboxColisao = new Rectangle(0, 0, 1f, 1f);
        this.renderObj = new ObjetoRenderizavel();
    }

    public void init(Vector2 posicaoInicial, Texture texturaPronta) {
        this.posicaoMundo.set(posicaoInicial);
        this.localStateTime = 0f;
        this.isAtivo = true;
        this.timerMorto = 0f;
        this.vida = 3; // Reseta a vida

        atualizarHitboxLogica();

        if (animacaoIdle == null) {
            carregarAnimacoes(texturaPronta);
        }
    }

    private void carregarAnimacoes(Texture sheet) {
        int larguraFrame = sheet.getWidth() / quantidade_frames;
        int alturaFrame = sheet.getHeight();

        TextureRegion[][] tmpStrip = TextureRegion.split(sheet, larguraFrame, alturaFrame);
        TextureRegion[] frames = new TextureRegion[quantidade_frames];
        System.arraycopy(tmpStrip[0], 0, frames, 0, quantidade_frames);

        animacaoIdle = new Animation<>(0.1f, frames);
        animacaoIdle.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta, Vector2 posicaoPlayer, Array<Morcego> bando, float limiteX, float limiteY) {
        if (!isAtivo) {
            timerMorto += delta;
            if (timerMorto >= tempo_respawn) {
                respawn(limiteX, limiteY);
            }
            return;
        }

        localStateTime += delta;
        renderObj.textura = animacaoIdle.getKeyFrame(localStateTime, true);

        Vector2 direcaoAoPlayer = new Vector2(posicaoPlayer.x - posicaoMundo.x, posicaoPlayer.y - posicaoMundo.y);
        float distanciaPlayer = direcaoAoPlayer.len();
        Vector2 forcaTotal = new Vector2();

        if (distanciaPlayer > 0.8f) {
            forcaTotal.add(direcaoAoPlayer.nor());
        }

        Vector2 separacao = new Vector2();
        int vizinhosMuitoPerto = 0;
        float raio_de_separacao = 1.2f;

        for (int i = 0; i < bando.size; i++) {
            Morcego outro = bando.get(i);
            if (outro != this && outro.isAtivo) {
                float distanciaAmigo = posicaoMundo.dst(outro.posicaoMundo);
                if (distanciaAmigo < raio_de_separacao && distanciaAmigo > 0) {
                    Vector2 repulsao = new Vector2(posicaoMundo.x - outro.posicaoMundo.x, posicaoMundo.y - outro.posicaoMundo.y);
                    repulsao.nor().scl(1f / distanciaAmigo);
                    separacao.add(repulsao);
                    vizinhosMuitoPerto++;
                }
            }
        }

        if (vizinhosMuitoPerto > 0) {
            forcaTotal.add(separacao.scl(1.5f));
        }

        if (!forcaTotal.isZero()) {
            if (forcaTotal.len() > 1f) forcaTotal.nor();
            float multiplicadorVelocidade = 1f;
            if (distanciaPlayer < 2.0f) {
                multiplicadorVelocidade = Math.max(0.1f, distanciaPlayer - 0.8f);
            }
            posicaoMundo.mulAdd(forcaTotal, velocidade * multiplicadorVelocidade * delta);
        }

        atualizarHitboxLogica();
    }

    public void tomarDano() {
        vida--;
        if (vida <= 0) {
            isAtivo = false;
            timerMorto = 0f;
        }
    }

    private void respawn(float limiteX, float limiteY) {
        float px = MathUtils.random(2f, limiteY - 2f);
        float py = MathUtils.random(-limiteX + 2f, -2f);
        posicaoMundo.set(px, py);
        atualizarHitboxLogica();

        vida = 3;
        isAtivo = true;
    }

    private void atualizarHitboxLogica() {
        hitboxColisao.setPosition(
            posicaoMundo.x + (hitboxColisao.width / 2f) + 2f,
            posicaoMundo.y + (hitboxColisao.height / 2f) + 2f
        );
    }

    public void prepararZSorting(float screenX, float screenY) {
        renderObj.sortY = screenY;
        renderObj.drawX = screenX - (64f / 2f);
        renderObj.drawY = screenY + elevacao_visual;
    }

    @Override
    public void reset() {
        isAtivo = false;
        renderObj.textura = null;
        vida = 3;
    }
}
