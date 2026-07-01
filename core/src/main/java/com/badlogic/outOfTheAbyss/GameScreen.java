package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Comparator;

public class GameScreen implements Screen {

    final JogoIsometrico game;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;

    OrthographicCamera camera;
    Viewport viewport;
    final float viewport_width = 640;
    final float viewport_height = 360;

    OrthographicCamera uiCamera;
    Viewport uiViewport;
    BitmapFont font;

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

    // SISTEMA DE LUZ E NÉVOA (FOG OF WAR)
    FrameBuffer lightBuffer;
    TextureRegion lightBufferRegion;
    Texture lightBrush;

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
    private float timerRespawnMorcego = 0f;
    private final int max_morcegos_no_mapa = 10;
    private final Texture textureMorcegoFly;

    Array<Pedra> pedrasDoMapa = new Array<>();
    int quantidade_pedras = 50;

    private final Vector3 auxMousePos = new Vector3();

    public GameScreen(final JogoIsometrico game) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;

        this.font.getData().setScale(1f);
        this.font.setUseIntegerPositions(true);

        camera = new OrthographicCamera();
        viewport = new FitViewport(viewport_width, viewport_height, camera);
        shapeRenderer = new ShapeRenderer();

        uiCamera = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCamera);

        mapaTexture = game.assets.get("mapa/mapa_simples.png", Texture.class);
        mapaOffsetY = -limiteMapaX * (tile_height / 2f);

        // INICIALIZAÇÃO DA LUZ
        // 1. Cria o FrameBuffer com a exata resolução do nosso Viewport Virtual
        lightBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int)viewport_width, (int)viewport_height, false);
        lightBufferRegion = new TextureRegion(lightBuffer.getColorBufferTexture());
        lightBufferRegion.flip(false, true); // O OpenGL renderiza FBOs de cabeça para baixo nativamente

        // 2. Cria a textura do feixe de luz usando a RAM (Sem depender de arquivos PNG novos)
        lightBrush = gerarTexturaLuz(256);

        TextureRegion pedraRegion = new TextureRegion(game.assets.get("mapa/objetos/pedras/pedra_01.png", Texture.class));
        for (int i = 0; i < quantidade_pedras; i++) {
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);
            pedrasDoMapa.add(new Pedra(new Vector2(px, py), pedraRegion));
        }

        playerController = new PlayerController();
        Vector2 posicaoInicial = new Vector2(limiteMapaY / 2f, -limiteMapaX / 2f);
        player = new Player(posicaoInicial, game.assets, playerController);

        textureMorcegoFly = game.assets.get("inimigos/morcego/morcego_fly.png", Texture.class);
        for (int i = 0; i < max_morcegos_no_mapa; i++) {
            gerarMorcegoAleatorio();
        }
    }

    /**
     * Gera uma textura procedural de luz suave (gradiente radial) na CPU e a envia para a GPU.
     */
    private Texture gerarTexturaLuz(int tamanho) {
        Pixmap pixmap = new Pixmap(tamanho, tamanho, Pixmap.Format.RGBA8888);
        float raio = tamanho / 2f;

        for (int x = 0; x < tamanho; x++) {
            for (int y = 0; y < tamanho; y++) {
                float dist = Vector2.dst(x, y, raio, raio);
                float alpha = 1f - (dist / raio);
                if (alpha < 0f) alpha = 0f;

                // Atenuação Quadrática (Deixa a borda da luz muito mais natural que uma linha reta)
                alpha = alpha * alpha;
                pixmap.setColor(1f, 1f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture tex = new Texture(pixmap);
        pixmap.dispose(); // Best Practice: Sempre descartar Pixmaps nativos da RAM após enviar pra VRAM
        return tex;
    }

    @Override
    public void show() {
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

        if (playerController.consumeFullscreenToggle()) {
            if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(1280, 720);
            else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        if (playerController.consumeDebugInfoToggle()) mostrarDebugInfo = !mostrarDebugInfo;
        if (playerController.consumeHitboxesToggle()) mostrarHitboxes = !mostrarHitboxes;

        auxMousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(auxMousePos);

        player.updateInput(delta, pedrasDoMapa, limiteMapaX, limiteMapaY, auxMousePos.x, auxMousePos.y);
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

        if (morcegos.size < max_morcegos_no_mapa) {
            timerRespawnMorcego += delta;
            float tempo_respawn_morcego = 3.0f;
            if (timerRespawnMorcego >= tempo_respawn_morcego) {
                gerarMorcegoAleatorio();
                timerRespawnMorcego -= tempo_respawn_morcego;
            }
        }

        for (int i = morcegos.size - 1; i >= 0; i--) {
            Morcego morcego = morcegos.get(i);
            if (!morcego.isAtivo) {
                morcegos.removeIndex(i);
                morcegoPool.free(morcego);
                continue;
            }
            morcego.update(delta, player.posicaoMundo, morcegos, limiteMapaX, limiteMapaY);
        }

        float offsetCameraY = viewport_height / 4f;
        camera.position.set(screenX, screenY + offsetCameraY, 0);
        camera.update();
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapaTexture, mapaOffsetX, mapaOffsetY);

        listaDeDesenho.clear();
        player.atualizarRenderizacao(delta, screenX, screenY);
        player.renderObj.alpha = 1f;
        listaDeDesenho.add(player.renderObj);

        for (Morcego morcego : morcegos) {
            float mScreenX = (morcego.posicaoMundo.x - morcego.posicaoMundo.y) * (tile_width / 2f);
            float mScreenY = (morcego.posicaoMundo.x + morcego.posicaoMundo.y) * (tile_height / 2f);
            morcego.prepararZSorting(mScreenX, mScreenY);
            morcego.renderObj.alpha = 1f;
            listaDeDesenho.add(morcego.renderObj);
        }

        for (Pedra pedra : pedrasDoMapa) {
            pedra.prepararZSorting(tile_width, tile_height);
            listaDeDesenho.add(pedra.renderObj);
        }

        for (SombraDash sombra : sombrasAtivas) listaDeDesenho.add(sombra.render);

        listaDeDesenho.sort(new Comparator<ObjetoRenderizavel>() {
            @Override
            public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
                return Float.compare(obj2.sortY, obj1.sortY);
            }
        });

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            if (obj.textura != null) {
                batch.setColor(1f, 1f, 1f, obj.alpha);
                batch.draw(obj.textura, obj.drawX, obj.drawY);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // ----------------------------------------------------
        // SISTEMA DE FOG E LUZ 2D (FRAMEBUFFER E BLEND)
        // ----------------------------------------------------

        // 1. Inicia a interceptação de desenho para o nosso FrameBuffer (ao invés da tela do monitor)
        lightBuffer.begin();

        // Limpa o Framebuffer pintando-o com a Escuridão Ambiente (Azul bem escuro e opaco)
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 0.95f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // 2. Mistura Aditiva: Todas as luzes que desenharmos vão se SOMAR, tornando o pixel mais branco
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Define a luz do jogador
        float raioVisao = 500f;
        float luzX = screenX - (raioVisao / 2f);
        float luzY = screenY - (raioVisao / 2f) + (tile_height); // Leve offset Y para centralizar no peito do char

        // Desenha o carimbo de luz. Como a cor base do Pincel é Branca, os pixels desta região ficam = 1.0 (Brancos)
        batch.draw(lightBrush, luzX, luzY, raioVisao, raioVisao);

        // Dica: Se quiser que as pedras brilhem ou que inimigos emitam luz, basta fazer um loop desenhando o brush neles!

        batch.end();
        lightBuffer.end();

        // 3. Hora de aplicar a camada do FrameBuffer (Luzes + Escuridão) por cima do Mundo do Jogo já desenhado
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 4. Mistura Multiplicativa Escura
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);

        // Desenhamos a aba FBO amarrada na posição da nossa Camera para cobrir o monitor exatamente onde ele está olhando
        batch.draw(lightBufferRegion,
            camera.position.x - viewport.getWorldWidth() / 2f,
            camera.position.y - viewport.getWorldHeight() / 2f,
            viewport.getWorldWidth(), viewport.getWorldHeight());

        // 5. Devolvemos a configuração da placa de vídeo ao padrão tradicional de Transparência do LibGDX
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();

        // ----------------------------------------------------

        if (mostrarHitboxes) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            shapeRenderer.setColor(Color.YELLOW);
            for (Pedra pedra : pedrasDoMapa) desenharRetanguloIsometrico(pedra.hitboxColisao, shapeRenderer);

            shapeRenderer.setColor(Color.RED);
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo) desenharRetanguloIsometrico(morcego.hitboxColisao, shapeRenderer);
            }

            shapeRenderer.setColor(Color.GREEN);
            desenharRetanguloIsometrico(player.hitbox, shapeRenderer);

            if (player.estaAtacando) {
                shapeRenderer.setColor(Color.BLUE);
                desenharRetanguloIsometrico(player.hitboxAtaque, shapeRenderer);
            }
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo) {
                    float mScreenX = (morcego.posicaoMundo.x - morcego.posicaoMundo.y) * (tile_width / 2f);
                    float mScreenY = (morcego.posicaoMundo.x + morcego.posicaoMundo.y) * (tile_height / 2f);

                    float larguraBarra = 20f;
                    float alturaBarra = 3f;
                    float barraX = mScreenX - (larguraBarra / 2f);
                    float barraY = mScreenY + morcego.elevacao_visual + 20f;

                    shapeRenderer.setColor(Color.RED);
                    shapeRenderer.rect(barraX, barraY, larguraBarra, alturaBarra);

                    shapeRenderer.setColor(Color.GREEN);
                    float proporcaoVida = (float) morcego.vida / morcego.vida_maxima;
                    shapeRenderer.rect(barraX, barraY, larguraBarra * proporcaoVida, alturaBarra);
                }
            }
            shapeRenderer.end();
        }

        if (mostrarDebugInfo) {
            uiViewport.apply();
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();

            String textoOverlay = String.format(
                "FPS: %d\nPOS MUNDO:\nX: %.2f | Y: %.2f\nPOS TELA:\nX: %.1f | Y: %.1f",
                Gdx.graphics.getFramesPerSecond(), player.posicaoMundo.x, player.posicaoMundo.y, screenX, screenY
            );

            font.setColor(Color.GREEN);
            font.draw(batch, textoOverlay, 15, viewport_height - 15);
            batch.end();
        }
    }

    private void gerarMorcegoAleatorio() {
        float px = MathUtils.random(2f, limiteMapaY - 2f);
        float py = MathUtils.random(-limiteMapaX + 2f, -2f);
        Morcego m = morcegoPool.obtain();
        m.init(new Vector2(px, py), textureMorcegoFly);
        morcegos.add(m);
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
        uiViewport.update(width, height, true);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        // Best Practice: O Framebuffer e sua textura dinâmica criam alocações brutas fora do GC, devemos descarta-los.
        lightBuffer.dispose();
        lightBrush.dispose();
    }
}
