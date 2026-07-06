package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TelaCreditos implements Screen {
    private final JogoIsometrico game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private Music musicaCreditos;

    private enum Estado { FADE_IN, ESPERANDO_INPUT, FADE_OUT }
    private Estado estadoAtual = Estado.FADE_IN;
    private float alphaTexto = 0f;
    private final float VELOCIDADE_FADE = 0.5f;

    public TelaCreditos(final JogoIsometrico game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(640, 360, camera);
        this.musicaCreditos = game.assets.get("sons/The Black Mirror.wav", Music.class);
    }

    @Override
    public void show() {
        if (musicaCreditos != null) {
            musicaCreditos.setLooping(true);
            musicaCreditos.setVolume(1.0f);
            musicaCreditos.play();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Máquina de estados da transição
        switch (estadoAtual) {
            case FADE_IN:
                alphaTexto += delta * VELOCIDADE_FADE;
                if (alphaTexto >= 1f) {
                    alphaTexto = 1f;
                    estadoAtual = Estado.ESPERANDO_INPUT;
                }
                break;

            case ESPERANDO_INPUT:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched()) {
                    estadoAtual = Estado.FADE_OUT;
                }
                break;

            case FADE_OUT:
                alphaTexto -= delta * VELOCIDADE_FADE;

                if (musicaCreditos != null && musicaCreditos.isPlaying()) {
                    musicaCreditos.setVolume(Math.max(0f, alphaTexto));
                }

                if (alphaTexto <= 0f) {
                    if (musicaCreditos != null) musicaCreditos.stop();

                    game.setScreen(new MenuInicial(game));
                    dispose();
                    return;
                }
                break;
        }

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.batch.begin();

        float centroY = viewport.getWorldHeight() / 2f;
        float margemDireita = viewport.getWorldWidth();

        // Título de Agradecimento (Amarelo/Dourado)
        game.fontTelas.getData().setScale(1.5f);
        game.fontTelas.setColor(1f, 0.8f, 0.2f, Math.max(0f, alphaTexto));
        game.fontTelas.draw(game.batch, "OBRIGADO POR JOGAR!", 0, centroY + 100f, margemDireita, Align.center, false);

        // Restaurar escala para os nomes
        game.fontTelas.getData().setScale(1.0f);
        game.fontTelas.setColor(1f, 1f, 1f, Math.max(0f, alphaTexto));

        game.fontTelas.draw(game.batch, "Desenvolvido por:", 0, centroY + 40f, margemDireita, Align.center, false);
        game.fontTelas.setColor(0.8f, 0.8f, 0.8f, Math.max(0f, alphaTexto));
        game.fontTelas.draw(game.batch, "Matheus Dall olmo", 0, centroY, margemDireita, Align.center, false);
        game.fontTelas.draw(game.batch, "Pablo Gabriel Sustisso", 0, centroY - 30f, margemDireita, Align.center, false);
        game.fontTelas.draw(game.batch, "Samuel Grontoski", 0, centroY - 60f, margemDireita, Align.center, false);

        // "Pressione qualquer tecla" piscando
        if (estadoAtual == Estado.ESPERANDO_INPUT) {
            float pulso = 0.5f + (float)(Math.sin(System.currentTimeMillis() / 300.0) * 0.5);
            game.fontTelas.setColor(1f, 1f, 1f, pulso);
        } else if (estadoAtual == Estado.FADE_OUT) {
            float pulso = 0.5f + (float)(Math.sin(System.currentTimeMillis() / 300.0) * 0.5);
            game.fontTelas.setColor(1f, 1f, 1f, Math.max(0f, alphaTexto) * pulso);
        } else {
            game.fontTelas.setColor(1f, 1f, 1f, Math.max(0f, alphaTexto));
        }

        game.fontTelas.draw(game.batch, "Pressione qualquer tecla", 0, 40f, margemDireita, Align.center, false);

        game.fontTelas.setColor(Color.WHITE);
        game.batch.setColor(Color.WHITE);

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
