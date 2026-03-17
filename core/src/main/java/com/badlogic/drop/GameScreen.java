package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class GameScreen implements Screen {
    final JogoIsometrico game; // Assumo que JogoIsometrico tem um viewport/batch, mas criaremos locais aqui para não depender

    OrthographicCamera camera;
    TiledMap map;
    IsometricTiledMapRenderer renderer;

    SpriteBatch batch;
    Texture playerSprite;

    // Posição do jogador no "Mundo" (coordenadas do grid isométrico)
    Vector2 playerMundo;
    float velocidade = 5f;

    // Tamanho do tile do seu mapa (do Tiled)
    final float TILE_WIDTH = 32f;
    final float TILE_HEIGHT = 16f;

    float limiteMapaX;
    float limiteMapaY;

    // Variáveis auxiliares para a tela
    float screenX;
    float screenY;

    public GameScreen(final JogoIsometrico game) {
        this.game = game;

        // 1. Configurar a Câmera
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        camera.position.set(0, 0, 0);
        camera.update();

        // 2. Carregar o Mapa do Tiled
        map = new TmxMapLoader().load("mapa_simples.tmx");

        int mapWidth = map.getProperties().get("width", Integer.class);
        int mapHeight = map.getProperties().get("height", Integer.class);
        limiteMapaX = (float) mapWidth;
        limiteMapaY = (float) mapHeight;

        // 3. Criar Renderizador
        renderer = new IsometricTiledMapRenderer(map);

        batch = new SpriteBatch();
        playerSprite = new Texture("personagem.png");

        playerMundo = new Vector2(limiteMapaY / 2f, -limiteMapaX / 2f);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        input(delta);
        logic();
        draw();
    }

    private void input(float delta) {
        float moveSpeed = velocidade * delta;

        // Movimento nas coordenadas isométricas do mundo
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerMundo.x += moveSpeed;
            playerMundo.y += moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerMundo.x -= moveSpeed;
            playerMundo.y -= moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerMundo.x += moveSpeed;
            playerMundo.y -= moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerMundo.x -= moveSpeed;
            playerMundo.y += moveSpeed;
        }
    }

    private void logic() {
        // Colisão com as bordas do mapa (grid do Tiled)
        playerMundo.x = MathUtils.clamp(playerMundo.x, 0, limiteMapaY);
        playerMundo.y = MathUtils.clamp(playerMundo.y, -limiteMapaX, 0); // Ajuste: Tiled costuma usar Y negativo no isométrico

        // Converter Coordenadas do Mundo para a Tela
        screenX = (playerMundo.x - playerMundo.y) * (TILE_WIDTH / 2f);
        screenY = (playerMundo.x + playerMundo.y) * (TILE_HEIGHT / 2f);

        // Centralizar a Câmera no Jogador
        camera.position.set(screenX, screenY, 0);
        camera.update();
    }

    private void draw() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Desenhar o Mapa
        renderer.setView(camera);
        renderer.render();

        // 2. Desenhar o Personagem
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float drawX = screenX - (playerSprite.getWidth() / 2f);
        float drawY = screenY;

        batch.draw(playerSprite, drawX, drawY);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Atualiza a área visível da câmera se a janela for redimensionada
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        map.dispose();
        batch.dispose();
        playerSprite.dispose();
        renderer.dispose();
    }
}
