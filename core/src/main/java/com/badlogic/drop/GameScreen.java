package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {
    final JogoIsometrico game;

    OrthographicCamera camera;
    Viewport viewport;
    // Definindo a resolução fixa que o jogo vai enxergar
    final float VIEWPORT_WIDTH = 640f;
    final float VIEWPORT_HEIGHT = 360f;
    Texture mapaTexture;
    SpriteBatch batch;
    Texture playerSprite;

    // Posição do jogador no "Mundo" (coordenadas do grid isométrico)
    Vector2 playerMundo;
    float velocidade = 5f;
    Vector2 inputDirecao = new Vector2();

    // Tamanho do tile
    final float TILE_WIDTH = 32f;
    final float TILE_HEIGHT = 16f;

    // Limites da "Grade Virtual"
    float limiteMapaX;
    float limiteMapaY;

    // Variáveis auxiliares para a tela
    float screenX;
    float screenY;

    // Variáveis para alinhar o PNG com a matemática
    float mapaOffsetX;
    float mapaOffsetY;

    public GameScreen(final JogoIsometrico game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);

        mapaTexture = new Texture("mapa_simples.png");
        playerSprite = new Texture("personagem.png");
        batch = new SpriteBatch();

        int mapWidthTiles = 50;
        int mapHeightTiles = 40;

        limiteMapaX = (float) mapWidthTiles;
        limiteMapaY = (float) mapHeightTiles;

        mapaOffsetX = 0;
        mapaOffsetY = -limiteMapaX * (TILE_HEIGHT / 2f);

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
        float velocidadeAtual = velocidade;

        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
            velocidadeAtual = 7.5f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            velocidadeAtual = 1.5f;
        }

        float moveSpeed = velocidadeAtual * delta;
        inputDirecao.set(0, 0);

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            inputDirecao.x += 1;
            inputDirecao.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            inputDirecao.x -= 1;
            inputDirecao.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            inputDirecao.x += 1;
            inputDirecao.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            inputDirecao.x -= 1;
            inputDirecao.y += 1;
        }

        if (!inputDirecao.isZero()) {
            inputDirecao.nor();
            playerMundo.x += inputDirecao.x * moveSpeed;
            playerMundo.y += inputDirecao.y * moveSpeed;
        }
    }

    private void logic() {
        // Colisão com as bordas do mapa
        playerMundo.x = MathUtils.clamp(playerMundo.x, 0, limiteMapaY);
        playerMundo.y = MathUtils.clamp(playerMundo.y, -limiteMapaX, 0);

        // Converter Coordenadas do Mundo para a Tela
        screenX = (playerMundo.x - playerMundo.y) * (TILE_WIDTH / 2f);
        screenY = (playerMundo.x + playerMundo.y) * (TILE_HEIGHT / 2f);

        // Câmera no Jogador
        float offsetCameraY = VIEWPORT_HEIGHT / 4f;
        camera.position.set(screenX, screenY + offsetCameraY, 0);
        camera.update();
    }

    private void draw() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Desenha o Mapa em PNG
        batch.draw(mapaTexture, mapaOffsetX, mapaOffsetY);

        // Desenha o Personagem
        float drawX = screenX - (playerSprite.getWidth() / 2f);
        float drawY = screenY;

        batch.draw(playerSprite, drawX, drawY);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        mapaTexture.dispose();
        batch.dispose();
        playerSprite.dispose();
    }
}
