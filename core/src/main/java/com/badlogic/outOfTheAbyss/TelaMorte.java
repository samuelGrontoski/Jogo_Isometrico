package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound; // NOVO: Import do Sound
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TelaMorte implements Screen {
    private final JogoIsometrico game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private Sound somMorte; // NOVO: Variável para guardar o som

    private enum Estado { FADE_IN, ESPERANDO_INPUT, FADE_OUT }
    private Estado estadoAtual = Estado.FADE_IN;
    private float alphaTexto = 0f;
    private final float VELOCIDADE_FADE = 0.7f;

    public TelaMorte(final JogoIsometrico game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(640, 360, camera);
        this.somMorte = game.assets.get("sons/Die.mp3", Sound.class);
    }

    @Override
    public void show() {
        somMorte.play(1.0f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Lógica de Transição
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
                if (alphaTexto <= 0f) {
                    somMorte.stop();
                    game.setScreen(new TelaCarregamento(game));
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

        game.font.getData().setScale(2.5f);
        game.font.setColor(1f, 0f, 0f, Math.max(0f, alphaTexto));
        game.font.draw(game.batch, "VOCÊ MORREU!",
            0, centroY + 40f, viewport.getWorldWidth(), Align.center, false);

        game.font.getData().setScale(1.0f);
        game.font.setColor(1f, 1f, 1f, Math.max(0f, alphaTexto));
        game.font.draw(game.batch, "Pressione qualquer tecla",
            0, centroY - 40f, viewport.getWorldWidth(), Align.center, false);

        game.font.setColor(1f, 1f, 1f, 1f);
        game.batch.setColor(1f, 1f, 1f, 1f);

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        // Sons gerenciados pelo AssetManager não precisam de dispose individual aqui,
        // a JogoIsometrico.java cuida disso quando o jogo inteiro for fechado.
    }
}
