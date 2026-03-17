package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {
    final JogoIsometrico game;

    OrthographicCamera camera;
    Viewport viewport;
    final float VIEWPORT_WIDTH = 640f;
    final float VIEWPORT_HEIGHT = 360f;
    Texture mapaTexture;
    SpriteBatch batch;

    // Idle
    Animation<TextureRegion> idleAnimation;
    Texture idleSheet; //
    float stateTime; //
    final int QUANTIDADE_FRAMES_IDLE = 6;
    float tempoEsperaIdle = 1.6f;
    float duracaoAnimacaoIdle;
    float tempoCicloIdle;

    // Correndo
    Animation<TextureRegion> runAnimation;
    Texture runSheet;
    final int QUANTIDADE_FRAMES_RUN = 6;

    boolean isMoving = false;
    boolean wasMoving = false;

    // Posição do jogador no "Mundo" (coordenadas do grid isométrico)
    Vector2 playerMundo;
    float velocidade = 7.5f;
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

        mapaTexture = new Texture("mapa/mapa_simples.png");
        batch = new SpriteBatch();

        // Idle
        idleSheet = new Texture("personagem/personagem_idle.png");

        TextureRegion[][] tmp = TextureRegion.split(idleSheet,
            idleSheet.getWidth() / QUANTIDADE_FRAMES_IDLE,
            idleSheet.getHeight() / 1);

        TextureRegion[] idleFrames = new TextureRegion[QUANTIDADE_FRAMES_IDLE];
        int index = 0;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < QUANTIDADE_FRAMES_IDLE; j++) {
                idleFrames[index++] = tmp[i][j];
            }
        }

        idleAnimation = new Animation<TextureRegion>(0.1f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.NORMAL);
        duracaoAnimacaoIdle = idleAnimation.getAnimationDuration();
        tempoCicloIdle = duracaoAnimacaoIdle + tempoEsperaIdle;
        stateTime = 0f;

        // Correndo
        runSheet = new Texture("personagem/personagem_run_se.png");

        TextureRegion[][] tmpRun = TextureRegion.split(runSheet,
            runSheet.getWidth() / QUANTIDADE_FRAMES_RUN,
            runSheet.getHeight() / 1);

        TextureRegion[] runFrames = new TextureRegion[QUANTIDADE_FRAMES_RUN];
        int indexRun = 0;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < QUANTIDADE_FRAMES_RUN; j++) {
                runFrames[indexRun++] = tmpRun[i][j];
            }
        }

        runAnimation = new Animation<TextureRegion>(0.13f, runFrames);
        runAnimation.setPlayMode(Animation.PlayMode.LOOP);

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
        stateTime += delta;

        if (!input(delta)) {
            return;
        }
        logic();
        draw();
    }

    private boolean input(float delta) {
        float velocidadeAtual = velocidade;

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            velocidadeAtual = 2.5f;
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuInicial(game));
            dispose();
            return false;
        }

        return true;
    }

    private void logic() {
        // Detecta o estado de movimento
        isMoving = !inputDirecao.isZero();

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

        batch.draw(mapaTexture, mapaOffsetX, mapaOffsetY);

        // --- LÓGICA DE TROCA DE ANIMAÇÃO ---

        // Se o personagem acabou de começar a andar, ou acabou de parar, zeramos o cronômetro!
        if (isMoving != wasMoving) {
            stateTime = 0f;
            wasMoving = isMoving;
        }

        TextureRegion currentFrame;

        if (isMoving) {
            // Se estiver andando:
            // Pegamos o frame da animação de corrida (o true força o loop se o stateTime passar do tempo)
            currentFrame = runAnimation.getKeyFrame(stateTime, true);

        } else {
            // Se estiver parado:
            if (stateTime > tempoCicloIdle) {
                stateTime = 0f;
            }
            if (stateTime <= duracaoAnimacaoIdle) {
                currentFrame = idleAnimation.getKeyFrame(stateTime, false);
            } else {
                currentFrame = idleAnimation.getKeyFrame(0, false);
            }
        }

        // Desenha o frame
        float drawX = screenX - (currentFrame.getRegionWidth() / 2f);
        float drawY = screenY;

        batch.draw(currentFrame, drawX, drawY);

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
        idleSheet.dispose();
        runSheet.dispose();
    }
}
