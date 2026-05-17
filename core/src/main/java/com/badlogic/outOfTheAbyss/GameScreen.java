package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Comparator;

public class GameScreen implements Screen {

    final JogoIsometrico game;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;

    // Câmera e Viewport do Mundo Isométrico
    OrthographicCamera camera;
    Viewport viewport;
    final float viewport_width = 640f;
    final float viewport_height = 360f;

    // Câmera e Viewport Fixos para a Interface (UI) e Debug
    OrthographicCamera uiCamera;
    Viewport uiViewport;
    BitmapFont font;

    // Estados controladores de Debug
    private boolean mostrarDebugInfo = false;
    private boolean mostrarHitboxes = false;

    Array<ObjetoRenderizavel> listaDeDesenho = new Array<>();

    Texture mapaTexture;
    final float tile_width = 32f;
    final float tile_height = 16f;
    float limiteMapaX = 50f;
    float limiteMapaY = 50f;
    float mapaOffsetX = 0;
    float mapaOffsetY;

    float screenX, screenY;

    Player player;
    PlayerController playerController;

    private final Pool<SombraDash> sombraPool = new Pool<SombraDash>() {
        @Override
        protected SombraDash newObject() { return new SombraDash(); }
    };
    Array<SombraDash> sombrasAtivas = new Array<>();
    float tempoCriarProximaSombra = 0f;
    final float intervalo_sombras = 0.03f;

    private final Pool<Morcego> morcegoPool = new Pool<Morcego>() {
        @Override
        protected Morcego newObject() { return new Morcego(); }
    };
    Array<Morcego> morcegos = new Array<>();
    Array<Pedra> pedrasDoMapa = new Array<>();
    int quantidade_pedras = 50;

    public GameScreen(final JogoIsometrico game) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;

        // Setup do Mundo
        camera = new OrthographicCamera();
        viewport = new FitViewport(viewport_width, viewport_height, camera);
        shapeRenderer = new ShapeRenderer();

        // Setup Exclusivo da UI (Interface Fixo) - CORRIGIDO
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(viewport_width, viewport_height, uiCamera);

        mapaTexture = game.assets.get("mapa/mapa_simples.png", Texture.class);
        mapaOffsetY = -limiteMapaX * (tile_height / 2f);

        TextureRegion pedraRegion = new TextureRegion(game.assets.get("mapa/objetos/pedras/pedra_01.png", Texture.class));
        for (int i = 0; i < quantidade_pedras; i++) {
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);
            pedrasDoMapa.add(new Pedra(new Vector2(px, py), pedraRegion));
        }

        playerController = new PlayerController();
        Vector2 posicaoInicial = new Vector2(limiteMapaY / 2f, -limiteMapaX / 2f);
        player = new Player(posicaoInicial, game.assets, playerController);

        Texture textureMorcegoFly = game.assets.get("inimigos/morcego/morcego_fly.png", Texture.class);
        for (int i = 0; i < 10; i++) {
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);
            Morcego m = morcegoPool.obtain();
            m.init(new Vector2(px, py), textureMorcegoFly);
            morcegos.add(m);
        }
    }

    @Override
    public void show() {
        // Melhores práticas do libGDX: Adicionamos o InputProcessor
        // no show() para garantir que a tela foque ao ser carregada.
        Gdx.input.setInputProcessor(playerController);
    }

    @Override
    public void render(float delta) {
        if (!input(delta)) return;
        logic(delta);
        draw(delta);
    }

    private boolean input(float delta) {
        if (playerController.escapePressed) {
            game.setScreen(new MenuInicial(game));
            dispose();
            return false;
        }

        // LÓGICA DE ALTERNÂNCIA DE ECRÃ COMPLETO (F11)
        if (playerController.consumeFullscreenToggle()) {
            if (Gdx.graphics.isFullscreen()) {
                // Se estiver em Tela Cheia, reverte para o modo Janela
                Gdx.graphics.setWindowedMode(640, 360);
            } else {
                // Caso contrário, ativa a Tela Cheia baseando-se no monitor atual
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }

        // Toggles das interfaces de debug
        if (playerController.consumeDebugInfoToggle()) mostrarDebugInfo = !mostrarDebugInfo;
        if (playerController.consumeHitboxesToggle()) mostrarHitboxes = !mostrarHitboxes;

        player.updateInput(delta, pedrasDoMapa, limiteMapaX, limiteMapaY);
        return true;
    }

    private void logic(float delta) {
        player.atualizarLogicaAtaque(delta, morcegos);

        screenX = (player.posicaoMundo.x - player.posicaoMundo.y) * (tile_width / 2f);
        screenY = (player.posicaoMundo.x + player.posicaoMundo.y) * (tile_height / 2f);

        if (player.estaDandoDash) {
            tempoCriarProximaSombra -= delta;
            if (tempoCriarProximaSombra <= 0) {
                SombraDash novaSombra = sombraPool.obtain();
                novaSombra.render.textura = player.renderObj.textura;
                novaSombra.render.drawX = player.renderObj.drawX;
                novaSombra.render.drawY = player.renderObj.drawY;
                novaSombra.render.sortY = player.renderObj.sortY;
                novaSombra.render.alpha = 0.5f;
                novaSombra.tempoDeVida = novaSombra.tempo_max_vida;

                sombrasAtivas.add(novaSombra);
                tempoCriarProximaSombra = intervalo_sombras;
            }
        }

        for (int i = sombrasAtivas.size - 1; i >= 0; i--) {
            SombraDash sombra = sombrasAtivas.get(i);
            sombra.tempoDeVida -= delta;
            if (sombra.tempoDeVida <= 0) {
                sombrasAtivas.removeIndex(i);
                sombraPool.free(sombra);
            } else {
                sombra.render.alpha = (sombra.tempoDeVida / sombra.tempo_max_vida) * 0.5f;
            }
        }

        float offsetCameraY = viewport_height / 4f;
        camera.position.set(screenX, screenY + offsetCameraY, 0);
        camera.update();
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. RENDERIZAÇÃO DO MUNDO
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapaTexture, mapaOffsetX, mapaOffsetY);

        listaDeDesenho.clear();
        player.atualizarRenderizacao(delta, screenX, screenY);
        player.renderObj.alpha = 1f;
        listaDeDesenho.add(player.renderObj);

        for (Morcego morcego : morcegos) {
            morcego.update(delta, player.posicaoMundo, morcegos, limiteMapaX, limiteMapaY);
            if (morcego.isAtivo) {
                float mScreenX = (morcego.posicaoMundo.x - morcego.posicaoMundo.y) * (tile_width / 2f);
                float mScreenY = (morcego.posicaoMundo.x + morcego.posicaoMundo.y) * (tile_height / 2f);
                morcego.prepararZSorting(mScreenX, mScreenY);
                morcego.renderObj.alpha = 1f;
                listaDeDesenho.add(morcego.renderObj);
            }
        }

        for (Pedra pedra : pedrasDoMapa) {
            pedra.prepararZSorting(tile_width, tile_height);
            listaDeDesenho.add(pedra.renderObj);
        }

        for (SombraDash sombra : sombrasAtivas) {
            listaDeDesenho.add(sombra.render);
        }

        listaDeDesenho.sort(new Comparator<ObjetoRenderizavel>() {
            @Override
            public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
                return Float.compare(obj2.sortY, obj1.sortY);
            }
        });

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            batch.setColor(1f, 1f, 1f, obj.alpha);
            batch.draw(obj.textura, obj.drawX, obj.drawY);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // 2. RENDERIZAÇÃO DAS HITBOXES
        if (mostrarHitboxes) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            shapeRenderer.setColor(Color.YELLOW);
            for (Pedra pedra : pedrasDoMapa) {
                desenharRetanguloIsometrico(pedra.hitboxColisao, shapeRenderer);
            }

            shapeRenderer.setColor(Color.RED);
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo) {
                    desenharRetanguloIsometrico(morcego.hitboxColisao, shapeRenderer);
                }
            }

            shapeRenderer.setColor(Color.GREEN);
            desenharRetanguloIsometrico(player.hitbox, shapeRenderer);

            if (player.estaAtacando) {
                shapeRenderer.setColor(Color.BLUE);
                desenharRetanguloIsometrico(player.hitboxAtaque, shapeRenderer);
            }
            shapeRenderer.end();
        }

        // 3. RENDERIZAÇÃO DO OVERLAY DE DEBUG (F3)
        if (mostrarDebugInfo) {
            uiViewport.apply();
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();

            String textoOverlay = String.format(
                "FPS: %d\n" +
                    "POS MUNDO:\nX: %.2f | Y: %.2f\n" +
                    "POS TELA:\nX: %.1f | Y: %.1f",
                Gdx.graphics.getFramesPerSecond(),
                player.posicaoMundo.x, player.posicaoMundo.y,
                screenX, screenY
            );

            font.setColor(Color.GREEN);
            // Desenha a partir do topo para não ficar cortado se a janela mudar
            font.draw(batch, textoOverlay, 15, viewport_height - 15);
            batch.end();
        }
    }

    private void desenharRetanguloIsometrico(Rectangle rect, ShapeRenderer sr) {
        float x1 = rect.x, y1 = rect.y;
        float x2 = rect.x + rect.width, y2 = rect.y;
        float x3 = rect.x + rect.width, y3 = rect.y + rect.height;
        float x4 = rect.x, y4 = rect.y + rect.height;

        float sx1 = (x1 - y1) * (tile_width / 2f); float sy1 = (x1 + y1) * (tile_height / 2f);
        float sx2 = (x2 - y2) * (tile_width / 2f); float sy2 = (x2 + y2) * (tile_height / 2f);
        float sx3 = (x3 - y3) * (tile_width / 2f); float sy3 = (x3 + y3) * (tile_height / 2f);
        float sx4 = (x4 - y4) * (tile_width / 2f); float sy4 = (x4 + y4) * (tile_height / 2f);

        sr.line(sx1, sy1, sx2, sy2);
        sr.line(sx2, sy2, sx3, sy3);
        sr.line(sx3, sy3, sx4, sy4);
        sr.line(sx4, sy4, sx1, sy1);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true); // Agora uiViewport existe e foi corretamente instanciado!
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
