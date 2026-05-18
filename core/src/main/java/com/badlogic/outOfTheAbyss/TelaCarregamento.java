package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TelaCarregamento implements Screen {
    private final JogoIsometrico game;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    public TelaCarregamento(final JogoIsometrico game) {
        this.game = game;

        this.shapeRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(640, 360, camera);
    }

    @Override
    public void show() {
        // Anula a entrada para evitar cliques acidentais durante o carregamento
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        // update() retorna true quando 100% da fila de assets for resolvida
        if (game.assets.update()) {
            game.setScreen(new MenuInicial(game));
            dispose();
            return;
        }

        float progresso = game.assets.getProgress();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Renderiza a barra de carregamento dinâmica preenchida
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float largura_barra = 400f;
        float posX = (640f - largura_barra) / 2f;
        float altura_barra = 20f;
        float posY = (360f - altura_barra) / 2f;

        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(posX, posY, largura_barra, altura_barra);

        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(posX, posY, largura_barra * progresso, altura_barra);

        shapeRenderer.end();
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
        shapeRenderer.dispose();
    }
}
