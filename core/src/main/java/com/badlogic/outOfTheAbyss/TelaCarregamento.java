package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TelaCarregamento implements Screen {
    private final JogoIsometrico game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Texture iconeCarregamento;

    private float tempoDecorrido = 0f;
    private final float TEMPO_MINIMO = 0.5f;

    // Máquina de Estados para a Transição Elden Ring
    private enum Estado { FADE_IN, PULSANDO, FADE_OUT }
    private Estado estadoAtual = Estado.FADE_IN;
    private float alphaSimbolo = 0f;
    private final float VELOCIDADE_FADE = 1.0f; // Mesma velocidade do menu

    public TelaCarregamento(final JogoIsometrico game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(640, 360, camera);
        this.iconeCarregamento = new Texture("../assets/mapa/Icones/boot-icn-difficulty-valere-easy.png");
    }

    @Override
    public void show() { Gdx.input.setInputProcessor(null); }

    @Override
    public void render(float delta) {
        // 1. Limita o delta para impedir pulos do símbolo na tela
        if (delta > 0.05f) delta = 0.05f;

        // 2. Garante o fundo totalmente preto ANTES de processar os assets pesados
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        tempoDecorrido += delta;
        boolean carregamentoFisicoConcluido = game.assets.update();

        // Lógica de Fases
        switch (estadoAtual) {
            case FADE_IN:
                alphaSimbolo += delta * VELOCIDADE_FADE;
                if (alphaSimbolo >= 1f) {
                    alphaSimbolo = 1f;
                    estadoAtual = Estado.PULSANDO;
                }
                break;
            case PULSANDO:
                if (carregamentoFisicoConcluido && tempoDecorrido >= TEMPO_MINIMO) {
                    estadoAtual = Estado.FADE_OUT;
                }
                break;
            case FADE_OUT:
                alphaSimbolo -= delta * VELOCIDADE_FADE;
                if (alphaSimbolo <= 0f) {
                    game.setScreen(new GameScreen(game));
                    dispose();
                    return;
                }
                break;
        }

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        float escalaPulso = 1.0f + 0.05f * MathUtils.sin(tempoDecorrido * 3f);
        float w = iconeCarregamento.getWidth();
        float h = iconeCarregamento.getHeight();
        float posX = (640f - w) / 2f;
        float posY = (360f - h) / 2f;
        float originX = w / 2f;
        float originY = h / 2f;

        // 3. Garante a mesclagem ligada no OpenGL
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.batch.begin();
        game.batch.setColor(1f, 1f, 1f, Math.max(0f, alphaSimbolo)); // Proteção contra alpha negativo

        game.batch.draw(
            iconeCarregamento, posX, posY, originX, originY, w, h,
            escalaPulso, escalaPulso, 0, 0, 0, (int)w, (int)h, false, false
        );

        game.batch.setColor(Color.WHITE);
        game.batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() { iconeCarregamento.dispose(); }
}
