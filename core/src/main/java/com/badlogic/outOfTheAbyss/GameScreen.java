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
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
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
    // Mantendo sua alteração de raio de visão/viewport
    final float viewport_width = 640;
    final float viewport_height = 360;

    OrthographicCamera uiCamera;
    Viewport uiViewport;
    BitmapFont font;

    private boolean mostrarDebugInfo = false;
    private boolean mostrarHitboxes = false;

    Array<ObjetoRenderizavel> listaDeDesenho = new Array<>();

    TiledMap mapaTiled;
    IsometricTiledMapRenderer mapRenderer;

    Array<Rectangle> hitboxesMapa = new Array<>();

    final float tile_width = 32f;
    final float tile_height = 16f;
    float limiteMapaX = 100f;
    float limiteMapaY = 100f;

    float screenX, screenY;

    Player player;
    PlayerController playerController;

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
    private final int max_morcegos_no_mapa = 0;
    private final Texture textureMorcegoFly;

    Array<ObjetoRenderizavel> elementosMapaRenderizaveis = new Array<>();

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

        mapaTiled = game.assets.get("mapa/map_cave.tmx", TiledMap.class);

        // 1. O loop agora varre automaticamente todas as camadas existentes ("ground", "limites_mapa", "paredes")
        // transladando todas para a esquerda de forma uníssona para manter alinhamento perfeito.
        for (MapLayer layer : mapaTiled.getLayers()) {
            layer.setOffsetX(-tile_width / 2f);
        }

        mapRenderer = new IsometricTiledMapRenderer(mapaTiled, batch);

        // 2. BUSCA DE COLISÕES: Modificado para ler estritamente os retângulos da camada de borda.
        TiledMapTileLayer limitesLayer = (TiledMapTileLayer) mapaTiled.getLayers().get("limites_mapa");

        if (limitesLayer != null) {
            for (int col = 0; col < limitesLayer.getWidth(); col++) {
                for (int row = 0; row < limitesLayer.getHeight(); row++) {
                    TiledMapTileLayer.Cell cell = limitesLayer.getCell(col, row);
                    if (cell != null && cell.getTile() != null) {
                        // Verifica o boolean personalizado que você colocou no tileset de borda
                        Boolean hasCollider = cell.getTile().getProperties().get("collider", Boolean.class);
                        if (hasCollider != null && hasCollider) {

                            // Traduz a coordenada da LibGDX para o seu mundo
                            float worldX = row;
                            float worldY = -col;

                            hitboxesMapa.add(new Rectangle(worldX, worldY, 1f, 1f));
                        }
                    }
                }
            }
        }

        // 3. EXTRAÇÃO DE MÚLTIPLAS CAMADAS PARA O Z-SORTING (Algoritmo do Pintor)
        // A ordem no Array dita a ordem de sobreposição física!
        String[] camadasZSort = {"paredes", "Cristal", "Teias", "Objetos_Cenario"};

        for (int i = 0; i < camadasZSort.length; i++) {
            String nomeCamada = camadasZSort[i];
            TiledMapTileLayer layer = (TiledMapTileLayer) mapaTiled.getLayers().get(nomeCamada);

            if (layer != null) {
                layer.setVisible(false);

                for (int col = 0; col < layer.getWidth(); col++) {
                    for (int row = 0; row < layer.getHeight(); row++) {
                        TiledMapTileLayer.Cell cell = layer.getCell(col, row);

                        if (cell != null && cell.getTile() != null) {
                            float worldX = row;
                            float worldY = -col;

                            float pScreenX = (worldX - worldY) * (tile_width / 2f);
                            float pScreenY = (worldX + worldY) * (tile_height / 2f);

                            ObjetoRenderizavel obj = new ObjetoRenderizavel();
                            obj.textura = cell.getTile().getTextureRegion();

                            obj.drawX = pScreenX - (tile_width / 2f) + cell.getTile().getOffsetX();
                            obj.drawY = pScreenY + cell.getTile().getOffsetY();

                            if (nomeCamada.equals("paredes")) {
                                obj.sortY = pScreenY;
                            } else {
                                obj.sortY = -100000f - i;
                            }

                            obj.isElementoMapa = true;
                            obj.zIndexMapa = i; // 0=Paredes, 1=Cristal, 2=Teias, 3=Objetos

                            obj.flipX = cell.getFlipHorizontally();
                            obj.flipY = cell.getFlipVertically();
                            obj.rotation = cell.getRotation();

                            elementosMapaRenderizaveis.add(obj);
                        }
                    }
                }
            }
        }

        lightBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int)viewport_width, (int)viewport_height, false);
        lightBufferRegion = new TextureRegion(lightBuffer.getColorBufferTexture());
        lightBufferRegion.flip(false, true);

        lightBrush = gerarTexturaLuz(256);

        playerController = new PlayerController();
        // Mantendo o local de nascimento que você estipulou
        Vector2 posicaoInicial = new Vector2(23f, -80f);
        player = new Player(posicaoInicial, game.assets, playerController);

        textureMorcegoFly = game.assets.get("inimigos/morcego/morcego_fly.png", Texture.class);
        for (int i = 0; i < max_morcegos_no_mapa; i++) {
            gerarMorcegoAleatorio();
        }
    }

    private Texture gerarTexturaLuz(int tamanho) {
        Pixmap pixmap = new Pixmap(tamanho, tamanho, Pixmap.Format.RGBA8888);
        float raio = tamanho / 2f;

        for (int x = 0; x < tamanho; x++) {
            for (int y = 0; y < tamanho; y++) {
                float dist = Vector2.dst(x, y, raio, raio);
                float alpha = 1f - (dist / raio);
                if (alpha < 0f) alpha = 0f;

                alpha = alpha * alpha;
                pixmap.setColor(1f, 1f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
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

        player.updateInput(delta, hitboxesMapa, limiteMapaX, limiteMapaY, auxMousePos.x, auxMousePos.y);
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

        mapRenderer.setView(camera);
        // O render() do IsometricTiledMapRenderer empilha perfeitamente e desenha
        // o "ground", o "limites_mapa" e as "paredes" obedecendo a ordem e a transparência.
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

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

        // Adiciona TODOS os elementos visíveis das 4 camadas de uma só vez
        for (ObjetoRenderizavel elementoMapa : elementosMapaRenderizaveis) {
            listaDeDesenho.add(elementoMapa);
        }

        for (SombraDash sombra : sombrasAtivas) listaDeDesenho.add(sombra.render);

        listaDeDesenho.sort(new Comparator<ObjetoRenderizavel>() {
            @Override
            public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
                int compareProfundidade = Float.compare(obj2.sortY, obj1.sortY);

                // Se as duas texturas caírem EXATAMENTE na mesma célula (empate) e forem mapa
                if (compareProfundidade == 0 && obj1.isElementoMapa && obj2.isElementoMapa) {
                    // Força a obediência da ordem da camada (Paredes antes de Teias, etc)
                    return Integer.compare(obj1.zIndexMapa, obj2.zIndexMapa);
                }

                // Se estiverem em células diferentes, obedece o 3D nativo normalmente
                return compareProfundidade;
            }
        });

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            if (obj.textura != null) {
                batch.setColor(1f, 1f, 1f, obj.alpha);

                if (obj.flipX || obj.flipY || obj.rotation != 0) {
                    float w = obj.textura.getRegionWidth();
                    float h = obj.textura.getRegionHeight();
                    float originX = w / 2f;
                    float originY = h / 2f;
                    float scaleX = obj.flipX ? -1f : 1f;
                    float scaleY = obj.flipY ? -1f : 1f;
                    float rot = obj.rotation * -90f;
                    batch.draw(obj.textura, obj.drawX, obj.drawY, originX, originY, w, h, scaleX, scaleY, rot);
                } else {
                    batch.draw(obj.textura, obj.drawX, obj.drawY);
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        lightBuffer.begin();
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 0.95f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Mantendo seu raio de visão gigante
        float raioVisao = 700f;
        float luzX = screenX - (raioVisao / 2f);
        float luzY = screenY - (raioVisao / 2f) + (tile_height);
        batch.draw(lightBrush, luzX, luzY, raioVisao, raioVisao);

        batch.end();
        lightBuffer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);
        batch.draw(lightBufferRegion,
            camera.position.x - viewport.getWorldWidth() / 2f,
            camera.position.y - viewport.getWorldHeight() / 2f,
            viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();

        if (mostrarHitboxes) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            shapeRenderer.setColor(Color.MAGENTA);
            for (Rectangle mapRect : hitboxesMapa) {
                desenharRetanguloIsometrico(mapRect, shapeRenderer);
            }

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
        lightBuffer.dispose();
        lightBrush.dispose();
        mapaTiled.dispose();
    }
}
