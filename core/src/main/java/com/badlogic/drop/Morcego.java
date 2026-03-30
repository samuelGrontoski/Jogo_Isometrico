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
    final int quantidade_frames = 4;

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
        sheet = new Texture("inimigos/morcego/morcego_fly.png");

        int larguraFrame = sheet.getWidth() / quantidade_frames;
        int alturaFrame = sheet.getHeight();

        TextureRegion[][] tmpStrip = TextureRegion.split(sheet, larguraFrame, alturaFrame);
        TextureRegion[] frames = new TextureRegion[quantidade_frames];
        System.arraycopy(tmpStrip[0], 0, frames, 0, quantidade_frames);

        animacaoIdle = new Animation<>(0.1f, frames);
        animacaoIdle.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta) {
        localStateTime += delta;
        renderObj.textura = animacaoIdle.getKeyFrame(localStateTime, true);
    }

    private void atualizarHitboxLógica() {
        hitboxColisao.setPosition(
            posicaoMundo.x + (hitboxColisao.width / 2f) + 1f,
            posicaoMundo.y + (hitboxColisao.height / 2f) + 1f
        );
    }

    public void prepararZSorting(float screenX, float screenY) {
        renderObj.sortY = screenY;

        renderObj.drawX = screenX - (64f / 2f);
        renderObj.drawY = screenY + ELEVACAO_VISUAL;
    }

    public void dispose() {
        sheet.dispose();
    }
}
