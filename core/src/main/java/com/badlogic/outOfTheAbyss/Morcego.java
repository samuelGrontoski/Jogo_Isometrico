package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;

// Inteligência Artificial Boid com implemento Poolable para reutilização em massa.
public class Morcego implements Poolable {
    public boolean isAtivo = true;
    public int vida = 2;
    public final int vida_maxima = 2;

    public Vector2 posicaoMundo;
    public Rectangle hitboxColisao;
    public float velocidade = 3.5f;
    public final float raio_de_agro = 10.0f;
    public boolean detectouPlayer = false;
    public float attackCooldown = 1.0f;
    public final float tempo_recarga_ataque = 1.0f;

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

    // Pseudo-construtor: Injetado via `Pool.obtain()` para limpar estado de memórias recicladas
    public void init(Vector2 posicaoInicial, Texture texturaPronta) {
        this.posicaoMundo.set(posicaoInicial);
        this.localStateTime = 0f;
        this.isAtivo = true;
        this.vida = vida_maxima;
        this.detectouPlayer = false;
        this.attackCooldown = tempo_recarga_ataque;

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
        localStateTime += delta;
        renderObj.textura = animacaoIdle.getKeyFrame(localStateTime, true);

        // Algoritmo de IA Baseado em Forças
        Vector2 direcaoAoPlayer = new Vector2(posicaoPlayer.x - posicaoMundo.x, posicaoPlayer.y - posicaoMundo.y);
        float distanciaPlayer = direcaoAoPlayer.len();
        Vector2 forcaTotal = new Vector2();

        // --- COOLDOWN DO ATAQUE ---
        if (attackCooldown < tempo_recarga_ataque) {
            attackCooldown += delta;
        }

        // --- SISTEMA DE AGRO ---
        // Se o player entrar no raio de 5 tiles, o morcego detecta ele
        if (distanciaPlayer <= raio_de_agro) {
            detectouPlayer = true;
        }

        // Atração (Perseguição) - Só se move em direção ao player se já o detectou
        if (detectouPlayer && distanciaPlayer > 0.8f) {
            forcaTotal.add(direcaoAoPlayer.nor());
        }

        // Força de Separação (Mitiga colisão em rede de múltiplos atores simultâneos)
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
        }
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
        vida = vida_maxima;
        detectouPlayer = false;
        this.attackCooldown = tempo_recarga_ataque;
    }
}
