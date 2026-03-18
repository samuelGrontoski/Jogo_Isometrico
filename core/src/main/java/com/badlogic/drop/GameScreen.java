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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.Array;
import java.util.Comparator;

public class GameScreen implements Screen {
    final JogoIsometrico game;

    OrthographicCamera camera;
    Viewport viewport;
    final float VIEWPORT_WIDTH = 640f;
    final float VIEWPORT_HEIGHT = 360f;
    Texture mapaTexture;
    SpriteBatch batch;

    String direcaoAtual = "SE";

    // Idle
    Animation<TextureRegion> idleAnimationSE;
    Animation<TextureRegion> idleAnimationSW;
    Texture idleSheetSE;
    Texture idleSheetSW;

    float stateTime;
    final int QUANTIDADE_FRAMES_IDLE = 6;
    float tempoEsperaIdle = 1.6f;
    float duracaoAnimacaoIdle;
    float tempoCicloIdle;

    // Correndo
    Animation<TextureRegion> runAnimationSE;
    Animation<TextureRegion> runAnimationSW;
    Texture runSheetSE;
    Texture runSheetSW;
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

    // Variáveis de colisão
    Rectangle hitboxPlayer;
    ShapeRenderer shapeRenderer;

    Texture pedraTexture;

    // --- LÓGICA DE Z-SORTING ---
    Array<ObjetoRenderizavel> listaDeDesenho = new Array<>();
    ObjetoRenderizavel renderPlayer = new ObjetoRenderizavel();
    TextureRegion pedraRegion;

    Array<Rectangle> hitboxesPedras = new Array<>();
    Array<ObjetoRenderizavel> rendersPedras = new Array<>();
    int quantidadePedras = 50;

    // --- LÓGICA DE ATAQUE ---
    Rectangle hitboxAtaque = new Rectangle();
    boolean isAttacking = false;
    float attackTimer = 0f;
    final float ATTACK_DURATION = 0.2f;

    public GameScreen(final JogoIsometrico game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);

        mapaTexture = new Texture("mapa/mapa_simples.png");
        batch = new SpriteBatch();

        // ======================= IDLE =======================
        // Idle SE (Direita/Baixo)
        idleSheetSE = new Texture("personagem/personagem_idle_se.png");
        TextureRegion[][] tmpSE = TextureRegion.split(idleSheetSE, idleSheetSE.getWidth() / QUANTIDADE_FRAMES_IDLE, idleSheetSE.getHeight());
        TextureRegion[] idleFramesSE = new TextureRegion[QUANTIDADE_FRAMES_IDLE];
        System.arraycopy(tmpSE[0], 0, idleFramesSE, 0, QUANTIDADE_FRAMES_IDLE);
        idleAnimationSE = new Animation<TextureRegion>(0.1f, idleFramesSE);
        idleAnimationSE.setPlayMode(Animation.PlayMode.NORMAL);

        // Idle SW (Esquerda/Baixo)
        idleSheetSW = new Texture("personagem/personagem_idle_sw.png");
        TextureRegion[][] tmpSW = TextureRegion.split(idleSheetSW, idleSheetSW.getWidth() / QUANTIDADE_FRAMES_IDLE, idleSheetSW.getHeight());
        TextureRegion[] idleFramesSW = new TextureRegion[QUANTIDADE_FRAMES_IDLE];
        System.arraycopy(tmpSW[0], 0, idleFramesSW, 0, QUANTIDADE_FRAMES_IDLE);
        idleAnimationSW = new Animation<TextureRegion>(0.1f, idleFramesSW);
        idleAnimationSW.setPlayMode(Animation.PlayMode.NORMAL);

        // Tempos do Idle
        duracaoAnimacaoIdle = idleAnimationSE.getAnimationDuration();
        tempoCicloIdle = duracaoAnimacaoIdle + tempoEsperaIdle;
        stateTime = 0f;

        // ===================== CORRENDO =====================
        // Corrida SE
        runSheetSE = new Texture("personagem/personagem_run_se.png");
        TextureRegion[][] tmpRunSE = TextureRegion.split(runSheetSE, runSheetSE.getWidth() / QUANTIDADE_FRAMES_RUN, runSheetSE.getHeight());
        TextureRegion[] runFramesSE = new TextureRegion[QUANTIDADE_FRAMES_RUN];
        System.arraycopy(tmpRunSE[0], 0, runFramesSE, 0, QUANTIDADE_FRAMES_RUN);
        runAnimationSE = new Animation<TextureRegion>(0.13f, runFramesSE);
        runAnimationSE.setPlayMode(Animation.PlayMode.LOOP);

        // Corrida SW (NOVO)
        runSheetSW = new Texture("personagem/personagem_run_sw.png");
        TextureRegion[][] tmpRunSW = TextureRegion.split(runSheetSW, runSheetSW.getWidth() / QUANTIDADE_FRAMES_RUN, runSheetSW.getHeight());
        TextureRegion[] runFramesSW = new TextureRegion[QUANTIDADE_FRAMES_RUN];
        System.arraycopy(tmpRunSW[0], 0, runFramesSW, 0, QUANTIDADE_FRAMES_RUN);
        runAnimationSW = new Animation<TextureRegion>(0.13f, runFramesSW);
        runAnimationSW.setPlayMode(Animation.PlayMode.LOOP);

        int mapWidthTiles = 50;
        int mapHeightTiles = 50;

        limiteMapaX = (float) mapWidthTiles;
        limiteMapaY = (float) mapHeightTiles;

        mapaOffsetX = 0;
        mapaOffsetY = -limiteMapaX * (TILE_HEIGHT / 2f);

        shapeRenderer = new ShapeRenderer();
        hitboxPlayer = new Rectangle(10f, 0, 0.8f, 0.8f);

        pedraTexture = new Texture("mapa/objetos/pedras/pedra_01.png");
        pedraRegion = new TextureRegion(pedraTexture);

        // Gera pedras aleatórias pelo mapa
        for (int i = 0; i < quantidadePedras; i++) {
            // Sorteia uma posição X e Y dentro dos limites do mapa (com uma margem para não ficar na borda extrema)
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);

            // Cria a caixa de colisão da pedra e guarda na lista
            Rectangle novaPedra = new Rectangle(px, py, 1f, 1f);
            hitboxesPedras.add(novaPedra);

            // Já deixa o "pacote de desenho" dela pronto para economizar memória no loop de renderização
            ObjetoRenderizavel novoRender = new ObjetoRenderizavel();
            novoRender.textura = pedraRegion;
            rendersPedras.add(novoRender);
        }

        playerMundo = new Vector2(limiteMapaY / 2f, -limiteMapaX / 2f);
        hitboxPlayer.setPosition(playerMundo.x + (hitboxPlayer.width / 2f), playerMundo.y + (hitboxPlayer.height / 2f));
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

        if (!isAttacking) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                inputDirecao.x += 1;
                inputDirecao.y += 1;
                direcaoAtual = "N";
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                inputDirecao.x -= 1;
                inputDirecao.y -= 1;
                direcaoAtual = "S";
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                inputDirecao.x += 1;
                inputDirecao.y -= 1;
                direcaoAtual = "E";
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                inputDirecao.x -= 1;
                inputDirecao.y += 1;
                direcaoAtual = "W";
            }

            if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
                direcaoAtual = "NE";
            } else if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
                direcaoAtual = "NW";
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
                direcaoAtual = "SE";
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
                direcaoAtual = "SW";
            }
        }

        // --- GATILHO DO ATAQUE ---
        // Se clicar com o botão Esquerdo e não estiver atacando no momento
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttacking) {
            isAttacking = true;
            attackTimer = 0f;

            // Ponto central de onde o ataque vai nascer (começa no centro do jogador)
            float attackCenterX = playerMundo.x;
            float attackCenterY = playerMundo.y;

            // Desloca o ataque para a frente, dependendo de onde ele está olhando
            // (Em isométrico: SE = X+1 e Y-1 | SW = X-1 e Y+1)
            float alcance = 0.8f;
            switch (direcaoAtual) {
                // --- DIREÇÕES CARDEAIS (As 4 pontas do losango na tela) ---
                case "N": // Cima (W)
                    attackCenterX += alcance + 1f;
                    attackCenterY += alcance + 1f;
                    break;
                case "S": // Baixo (S)
                    attackCenterX -= alcance;
                    attackCenterY -= alcance;
                    break;
                case "E": // Direita (D)
                    attackCenterX += alcance;
                    attackCenterY -= alcance;
                    break;
                case "W": // Esquerda (A)
                    attackCenterX -= alcance;
                    attackCenterY += alcance;
                    break;

                // --- DIREÇÕES DIAGONAIS (As laterais retas do losango na tela) ---
                case "NE": // Cima-Direita (W + D)
                    attackCenterX += alcance;
                    // O Y se anula matematicamente (+alcance -alcance = 0)
                    break;
                case "NW": // Cima-Esquerda (W + A)
                    // O X se anula matematicamente
                    attackCenterY += alcance;
                    break;
                case "SE": // Baixo-Direita (S + D)
                    // O X se anula matematicamente
                    attackCenterY -= alcance;
                    break;
                case "SW": // Baixo-Esquerda (S + A)
                    attackCenterX -= alcance;
                    // O Y se anula matematicamente
                    break;
            }

            // Define a caixa de ataque (Tamanho 1.5x1.5 tiles, centralizada no ponto calculado)
            hitboxAtaque.set(attackCenterX - 0.75f, attackCenterY - 0.75f, 1.5f, 1.5f);
        }

        // Aplica o movimento com Colisão Preditiva
        if (!inputDirecao.isZero()) {
            inputDirecao.nor();

            // Guarda a posição ANTES de mover
            float oldX = playerMundo.x;
            float oldY = playerMundo.y;

            // --- TENTATIVA NO EIXO X ---
            playerMundo.x += inputDirecao.x * moveSpeed;
            hitboxPlayer.setPosition(playerMundo.x + (hitboxPlayer.width / 2f), playerMundo.y + (hitboxPlayer.height / 2f));

            for (Rectangle pedra : hitboxesPedras) {
                if (hitboxPlayer.overlaps(pedra)) {
                    playerMundo.x = oldX;
                    hitboxPlayer.setPosition(playerMundo.x - (hitboxPlayer.width / 2f), playerMundo.y - (hitboxPlayer.height / 2f));
                    break;
                }
            }

            // --- TENTATIVA NO EIXO Y ---
            playerMundo.y += inputDirecao.y * moveSpeed;
            hitboxPlayer.setPosition(playerMundo.x + (hitboxPlayer.width / 2f), playerMundo.y + (hitboxPlayer.height / 2f));

            for (Rectangle pedra : hitboxesPedras) {
                if (hitboxPlayer.overlaps(pedra)) {
                    playerMundo.y = oldY;
                    break;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuInicial(game));
            dispose();
            return false;
        }

        return true;
    }

    private void logic() {
        // --- LÓGICA DO ATAQUE E DESTRUIÇÃO ---
        if (isAttacking) {
            attackTimer += Gdx.graphics.getDeltaTime(); // Roda o relógio

            // Se o tempo acabou, desliga o ataque
            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
            } else {
                // Checa colisão com as pedras (Lendo DE TRÁS PARA FRENTE)
                for (int i = hitboxesPedras.size - 1; i >= 0; i--) {
                    if (hitboxAtaque.overlaps(hitboxesPedras.get(i))) {
                        // QUEBROU A PEDRA! Remove da lista de colisões e da lista de desenhos
                        hitboxesPedras.removeIndex(i);
                        rendersPedras.removeIndex(i);

                        // (Opcional) Podemos colocar um "break;" aqui se você quiser
                        // que 1 ataque quebre apenas 1 pedra por vez.
                        // Sem o break, a sua "espada" atravessa e quebra várias juntas!
                    }
                }
            }
        }

        // Detecta o estado de movimento
        isMoving = !inputDirecao.isZero();

        float margemPlayer = 1.5f;

        // Colisão com as bordas do mapa
        playerMundo.x = MathUtils.clamp(playerMundo.x, 0, limiteMapaY - margemPlayer);
        playerMundo.y = MathUtils.clamp(playerMundo.y, -limiteMapaX, -margemPlayer);

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

        // LÓGICA DE Z-SORTING (LISTA ORDENÁVEL)

        listaDeDesenho.clear();

        // Se o personagem mudou de estado (andou -> parou, parou -> andou), reseta o tempo
        if (isMoving != wasMoving) {
            stateTime = 0f;
            wasMoving = isMoving;
        }

        TextureRegion currentFrame;

        if (isMoving) {
            Animation<TextureRegion> animacaoRunCerta;
            if (direcaoAtual.equals("SW") || direcaoAtual.equals("S") || direcaoAtual.equals("NW") || direcaoAtual.equals("W")) {
                animacaoRunCerta = runAnimationSW;
            } else {
                animacaoRunCerta = runAnimationSE;
            }
            currentFrame = animacaoRunCerta.getKeyFrame(stateTime, true);

        } else {
            // Escolhe a animação correta com base na direção
            Animation<TextureRegion> animacaoIdleCerta;
            if (direcaoAtual.equals("SW") || direcaoAtual.equals("S") || direcaoAtual.equals("NW") || direcaoAtual.equals("W")) {
                animacaoIdleCerta = idleAnimationSW;
            } else {
                animacaoIdleCerta = idleAnimationSE;
            }

            // Lógica de pausa do Idle
            if (stateTime > tempoCicloIdle) {
                stateTime = 0f;
            }
            if (stateTime <= duracaoAnimacaoIdle) {
                currentFrame = animacaoIdleCerta.getKeyFrame(stateTime, false);
            } else {
                currentFrame = animacaoIdleCerta.getKeyFrame(0, false);
            }
        }

        // Calcula a posição do personagem na tela
        float playerDrawX = screenX - (currentFrame.getRegionWidth() / 2f);
        float playerDrawY = screenY;

        renderPlayer.textura = currentFrame;
        renderPlayer.drawX = playerDrawX;
        renderPlayer.drawY = playerDrawY;
        renderPlayer.sortY = screenY;
        listaDeDesenho.add(renderPlayer);

        // Prepara TODAS AS PEDRAS e joga na lista
        for (int i = 0; i < hitboxesPedras.size; i++) {
            Rectangle pedra = hitboxesPedras.get(i);
            ObjetoRenderizavel render = rendersPedras.get(i);

            float pScreenX = (pedra.x - pedra.y) * (TILE_WIDTH / 2f);
            float pScreenY = (pedra.x + pedra.y) * (TILE_HEIGHT / 2f);

            render.drawX = pScreenX - (pedraTexture.getWidth() / 2f);
            render.drawY = pScreenY;
            render.sortY = pScreenY;

            listaDeDesenho.add(render);
        }

        listaDeDesenho.sort(new Comparator<ObjetoRenderizavel>() {
            @Override
            public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
                return Float.compare(obj2.sortY, obj1.sortY);
            }
        });

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            batch.draw(obj.textura, obj.drawX, obj.drawY);
        }

        batch.end();

        // --- DEBUG DE COLISÃO (Linhas verdes e vermelhas) ---
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GREEN);
        for (Rectangle pedra : hitboxesPedras) {
            desenharRetanguloIsometrico(pedra, shapeRenderer);
        }
        shapeRenderer.setColor(Color.RED);
        desenharRetanguloIsometrico(hitboxPlayer, shapeRenderer);
        if (isAttacking) {
            shapeRenderer.setColor(Color.BLUE);
            desenharRetanguloIsometrico(hitboxAtaque, shapeRenderer);
        }
        shapeRenderer.end();
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
        idleSheetSE.dispose();
        idleSheetSW.dispose();
        runSheetSE.dispose();
        runSheetSW.dispose();
        pedraTexture.dispose();
    }

    private void desenharRetanguloIsometrico(Rectangle rect, ShapeRenderer sr) {
        // Pega os 4 cantos matemáticos da caixa
        float x1 = rect.x, y1 = rect.y;
        float x2 = rect.x + rect.width, y2 = rect.y;
        float x3 = rect.x + rect.width, y3 = rect.y + rect.height;
        float x4 = rect.x, y4 = rect.y + rect.height;

        // Converte os 4 cantos para a visão do monitor (Screen)
        float sx1 = (x1 - y1) * (TILE_WIDTH / 2f); float sy1 = (x1 + y1) * (TILE_HEIGHT / 2f);
        float sx2 = (x2 - y2) * (TILE_WIDTH / 2f); float sy2 = (x2 + y2) * (TILE_HEIGHT / 2f);
        float sx3 = (x3 - y3) * (TILE_WIDTH / 2f); float sy3 = (x3 + y3) * (TILE_HEIGHT / 2f);
        float sx4 = (x4 - y4) * (TILE_WIDTH / 2f); float sy4 = (x4 + y4) * (TILE_HEIGHT / 2f);

        // Desenha as 4 linhas conectando os pontos
        sr.line(sx1, sy1, sx2, sy2);
        sr.line(sx2, sy2, sx3, sy3);
        sr.line(sx3, sy3, sx4, sy4);
        sr.line(sx4, sy4, sx1, sy1);
    }

    class ObjetoRenderizavel {
        TextureRegion textura;
        float drawX;
        float drawY;
        float sortY;
    }
}
