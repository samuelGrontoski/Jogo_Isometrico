package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.*;
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

    private final Comparator<ObjetoRenderizavel> zIndexComparator = new Comparator<ObjetoRenderizavel>() {
        @Override
        public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
            int compareProfundidade = Float.compare(obj2.sortY, obj1.sortY);
            if (compareProfundidade == 0 && obj1.isElementoMapa && obj2.isElementoMapa) {
                return Integer.compare(obj1.zIndexMapa, obj2.zIndexMapa);
            }
            return compareProfundidade;
        }
    };

    TiledMap mapaTiled;
    IsometricTiledMapRenderer mapRenderer;
    TiledMapTileLayer limitesLayer;

    Array<Rectangle> hitboxesMapa = new Array<>();

    final float tile_width = 32f;
    final float tile_height = 16f;
    float limiteMapaX = 100f;
    float limiteMapaY = 100f;

    float screenX, screenY;

    Player player;
    PlayerController playerController;
    private Texture uiIconAtaqueLeve;
    private Texture uiIconAtaquePesado;
    private Texture uiIconDash;
    private Texture uiFrame;
    private Texture uiSombraCooldown;

    // --- LUZ E FOG ---
    FrameBuffer lightBuffer;
    TextureRegion lightBufferRegion;
    Texture lightBrush;

    private final Pool<Morcego> morcegoPool = new Pool<Morcego>() {
        @Override
        protected Morcego newObject() { return new Morcego(); }
    };
    Array<Morcego> morcegos = new Array<>();
    private float timerRespawnMorcego = 0f;
    private final int max_morcegos_no_mapa = 0;
    private final Texture textureMorcegoFly;

    Array<ObjetoRenderizavel> elementosMapaRenderizaveis = new Array<>();

    Array<Vector2> luzesVermelhas = new Array<>();
    Array<Vector2> luzesAzuis = new Array<>();

    private final Vector3 auxMousePos = new Vector3();

    // Variáveis da transição
    private float transicaoAlpha = 1f;
    private Texture pixelPreto;
    private final float VELOCIDADE_FADE = 1.0f;

    Array<Vector2> cristaisInterativos = new Array<>();
    private boolean pertoDoCristal = false;
    private boolean mostrandoMensagemMagica = false;
    private Texture portraitAephorul;

    // --- VARIÁVEIS DA MÚSICA E GATILHOS ---
    private Music musicaFundo;
    private Music musicaBoss;
    private float volumeFundo = 1.0f;
    private float volumeBoss = 0f;
    Array<Polygon> gatilhosMusicaBoss = new Array<>();

    Boss boss;

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

        // INICIALIZAÇÃO DOS ÁUDIOS
        musicaFundo = game.assets.get("sons/Go Down.wav", Music.class);
        musicaFundo.setLooping(true);
        musicaFundo.setVolume(volumeFundo);
        musicaFundo.play();

        musicaBoss = game.assets.get("sons/Boss_music.mp3", Music.class);
        musicaBoss.setLooping(true);
        musicaBoss.setVolume(volumeBoss);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        pixelPreto = new Texture(pixmap);
        pixmap.dispose();

        mapaTiled = game.assets.get("mapa/map_cave.tmx", TiledMap.class);

        // 1. O loop agora varre automaticamente todas as camadas existentes ("ground", "limites_mapa", "paredes")
        // transladando todas para a esquerda de forma uníssona para manter alinhamento perfeito.
        for (MapLayer layer : mapaTiled.getLayers()) {
            layer.setOffsetX(-tile_width / 2f);
        }

        mapRenderer = new IsometricTiledMapRenderer(mapaTiled, batch);

        // 2. BUSCA DE COLISÕES: Modificado para ler estritamente os retângulos da camada de borda.
        limitesLayer = (TiledMapTileLayer) mapaTiled.getLayers().get("limites_mapa");

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
        // A ordem no Array dita a ordem de sobreposição física
        String[] camadasZSort = {"simbolos_sala_boss", "paredes", "Cristal", "Teias", "Objetos_Cenario"};

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
                            obj.tile = cell.getTile();
                            obj.textura = cell.getTile().getTextureRegion();
                            obj.drawX = pScreenX - (tile_width / 2f) + cell.getTile().getOffsetX();
                            obj.drawY = pScreenY + cell.getTile().getOffsetY();

                            if (nomeCamada.equals("simbolos_sala_boss")) {
                                obj.sortY = 100000f;
                            } else if (nomeCamada.equals("Teias")) {
                                obj.sortY = -100000f - i;
                            } else {
                                obj.sortY = pScreenY - (i * 0.01f);
                            }

                            obj.isElementoMapa = true;
                            obj.zIndexMapa = i; // 0=Paredes, 1=Cristal, 2=Teias, 3=Objetos

                            Boolean emiteLuzVermelha = cell.getTile().getProperties().get("emiteLuzVermelha", Boolean.class);
                            if (emiteLuzVermelha != null && emiteLuzVermelha) {
                                float elevacaoPoste = 30f;
                                luzesVermelhas.add(new Vector2(pScreenX, pScreenY + elevacaoPoste));
                            }

                            Boolean emiteLuzAzul = cell.getTile().getProperties().get("emiteLuzAzul", Boolean.class);
                            if (emiteLuzAzul != null && emiteLuzAzul) {
                                float elevacaoCristal = 10f;
                                luzesAzuis.add(new Vector2(pScreenX, pScreenY + elevacaoCristal));
                            }

                            Boolean mostrarMensagem = cell.getTile().getProperties().get("mostrarMensagem", Boolean.class);
                            if (mostrarMensagem != null && mostrarMensagem) {
                                cristaisInterativos.add(new Vector2(worldX, worldY));
                            }

                            obj.flipX = cell.getFlipHorizontally();
                            obj.flipY = cell.getFlipVertically();
                            obj.rotation = cell.getRotation();

                            // NOVO: Pré-calcula TUDO na RAM durante a tela de loading.
                            obj.isTransformado = obj.flipX || obj.flipY || obj.rotation != 0;
                            if (obj.isTransformado) {
                                obj.width = obj.textura.getRegionWidth();
                                obj.height = obj.textura.getRegionHeight();
                                obj.originX = obj.width / 2f;
                                obj.originY = obj.height / 2f;
                                obj.scaleX = obj.flipX ? -1f : 1f;
                                obj.scaleY = obj.flipY ? -1f : 1f;
                                obj.grausRotacao = obj.rotation * -90f;
                            }

                            elementosMapaRenderizaveis.add(obj);
                        }
                    }
                }
            }
        }

        MapLayer layerGatilhos = mapaTiled.getLayers().get("Gatilhos");
        if (layerGatilhos != null) {
            for (MapObject objeto : layerGatilhos.getObjects()) {
                // AGORA LÊ POLÍGONOS!
                if (objeto instanceof PolygonMapObject) {
                    Object propMusica = objeto.getProperties().get("musica");
                    // Proteção caso tenha escrito com 'M' maiúsculo no Tiled
                    if (propMusica == null) propMusica = objeto.getProperties().get("Musica");

                    if (propMusica != null && "boss".equalsIgnoreCase(propMusica.toString().trim())) {
                        PolygonMapObject polyObj = (PolygonMapObject) objeto;

                        // Pega os pontos do polígono já convertidos para o ponto x,y dele no Tiled
                        float[] verticesTiled = polyObj.getPolygon().getTransformedVertices();
                        float[] verticesFisica = new float[verticesTiled.length];

                        // Converte cada ponto do polígono para a nossa matemática física!
                        for (int i = 0; i < verticesTiled.length; i += 2) {
                            float pX = verticesTiled[i];
                            float pY = verticesTiled[i + 1];

                            float tX = pX / tile_height;
                            float tY = pY / tile_height;

                            verticesFisica[i] = tY;        // worldX = TiledY
                            verticesFisica[i + 1] = -tX;   // worldY = -TiledX
                        }

                        gatilhosMusicaBoss.add(new Polygon(verticesFisica));
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
        Vector2 posicaoInicial = new Vector2(75f, -20f);
        player = new Player(posicaoInicial, game.assets, playerController);

        Vector2 posicaoInicialBoss = new Vector2(78f, -22f);
        boss = new Boss(posicaoInicialBoss, game.assets);

        textureMorcegoFly = game.assets.get("inimigos/morcego/morcego_fly.png", Texture.class);
        for (int i = 0; i < max_morcegos_no_mapa; i++) {
            gerarMorcegoAleatorio();
        }

        portraitAephorul = new Texture("portrait/dialog-portrait-Aephorul-Angry.png");

        // HUD Abilidades
        uiIconAtaqueLeve = game.assets.get("skills/ataque_leve_icon.png", Texture.class);
        uiIconAtaquePesado = game.assets.get("skills/ataque_pesado_icon.png", Texture.class);
        uiIconDash = game.assets.get("skills/dash_icon.png", Texture.class);
        uiFrame = game.assets.get("skills/frame_icon.png", Texture.class);

        // Criando um pixel preto com 75% de transparência (Alpha) para fazer o "overlay" do cooldown
        Pixmap pixmapHUD = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapHUD.setColor(0, 0, 0, 0.75f);
        pixmapHUD.fill();
        uiSombraCooldown = new Texture(pixmapHUD);
        pixmapHUD.dispose(); // Libera o pixmap da memória após gerar a textura
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
            musicaFundo.stop();
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

        // Toggle da Mensagem (Só funciona se estiver perto)
        if (pertoDoCristal && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            mostrandoMensagemMagica = !mostrandoMensagemMagica;
        }

        auxMousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(auxMousePos);

        player.updateInput(delta, hitboxesMapa, limiteMapaX, limiteMapaY, auxMousePos.x, auxMousePos.y);
        return true;
    }

    private void logic(float delta) {
        // --- SISTEMA DE INTERAÇÃO ---
        pertoDoCristal = false;

        for (Vector2 posCristal : cristaisInterativos) {
            // Se o jogador estiver a menos de 2 blocos de distância do cristal
            if (player.posicaoMundo.dst(posCristal) < 2.0f) {
                pertoDoCristal = true;
                break;
            }
        }

        // Se o jogador se afastar, fecha a mensagem automaticamente
        if (!pertoDoCristal) {
            mostrandoMensagemMagica = false;
        }
        // -----------------------------

        // --- SISTEMA DE CROSSFADE DA MÚSICA ---
        boolean naSalaDoBoss = false;
        float centroX = player.hitbox.x + (player.hitbox.width / 2f);
        float centroY = player.hitbox.y + (player.hitbox.height / 2f);

        for (Polygon gatilho : gatilhosMusicaBoss) {
            if (gatilho.contains(centroX, centroY)) {
                naSalaDoBoss = true;
                break;
            }
        }

        float velocidadeFade = 1.0f * delta; // Velocidade da transição (1 segundo para trocar)

        if (naSalaDoBoss) {
            if (!musicaBoss.isPlaying()) musicaBoss.play();
            // Aumenta o Boss, diminui a Caverna (Limitado em 1.0f)
            volumeBoss = Math.min(0.1f, volumeBoss + velocidadeFade);
            volumeFundo = Math.max(0f, volumeFundo - velocidadeFade);
        } else {
            if (!musicaFundo.isPlaying()) musicaFundo.play();
            // Aumenta a Caverna, diminui o Boss (Limitado em 1.0f)
            volumeFundo = Math.min(1.0f, volumeFundo + velocidadeFade);
            volumeBoss = Math.max(0f, volumeBoss - velocidadeFade);
        }

        musicaFundo.setVolume(volumeFundo);
        musicaBoss.setVolume(volumeBoss);

        // Pausa a música que ficou totalmente muda para economizar CPU
        if (volumeFundo <= 0f && musicaFundo.isPlaying()) musicaFundo.pause();
        if (volumeBoss <= 0f && musicaBoss.isPlaying()) musicaBoss.pause();
        // ----------------------------------------

        player.atualizarLogicaAtaque(delta, morcegos);

        if (boss != null) {
            boss.update(delta, player.posicaoMundo, player.hitbox, hitboxesMapa, limitesLayer.getHeight(), limitesLayer.getWidth());

            // --- CHECAGEM DE COLISÃO: TEIAS VS PLAYER ---
            for (TeiaProjetil teia : boss.teiasAtivas) {
                if (teia.hitbox.overlaps(player.hitbox)) {

                    if (teia.voando) {
                        // 1. O projetil bateu no player enquanto voava!
                        // Aqui você chama o método de dano do seu player. Exemplo:
                        // player.tomarDano(15);

                        // Força a teia a cair no chão imediatamente onde acertou o player
                        teia.voando = false;
                        teia.stateTime = 0f;
                        teia.posicaoMundo.set(player.posicaoMundo);
                        teia.hitbox.setPosition(teia.posicaoMundo.x, teia.posicaoMundo.y);
                    }
                    else {
                        // 2. A teia já está no chão e o player pisou nela (Armadilha)
                        // Aqui você pode aplicar um status de lentidão. Exemplo:
                        // player.aplicarLentidao(delta);
                    }
                }
            }
        }

        screenX = (player.posicaoMundo.x - player.posicaoMundo.y) * (tile_width / 2f);
        screenY = (player.posicaoMundo.x + player.posicaoMundo.y) * (tile_height / 2f);

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

        if (boss != null) {
            TextureRegion frame = boss.getCurrentFrame();
            if (frame != null) {
                ObjetoRenderizavel bossRender = new ObjetoRenderizavel();
                bossRender.textura = frame;
                bossRender.drawX = boss.getScreenX() - (frame.getRegionWidth() / 2f);
                bossRender.drawY = boss.getScreenY() - 175f; // + ajuste fino se precisar
                bossRender.sortY = boss.getScreenY();
                bossRender.alpha = 1f;
                bossRender.isElementoMapa = false;
                listaDeDesenho.add(bossRender);
            }
        }

        if (boss != null) {
            for (TeiaProjetil teia : boss.teiasAtivas) {
                TextureRegion frameTeia = teia.getCurrentFrame();
                if (frameTeia != null) {
                    float tScreenX = (teia.posicaoMundo.x - teia.posicaoMundo.y) * (tile_width / 2f);
                    float tScreenY = (teia.posicaoMundo.x + teia.posicaoMundo.y) * (tile_height / 2f);

                    ObjetoRenderizavel teiaRender = new ObjetoRenderizavel();
                    teiaRender.textura = frameTeia;
                    teiaRender.drawX = tScreenX - (frameTeia.getRegionWidth() / 2f);
                    teiaRender.drawY = tScreenY - 40f; // Ajuste fino da altura isométrica

                    teiaRender.sortY = tScreenY;
                    teiaRender.alpha = 1f;
                    teiaRender.isElementoMapa = false;
                    listaDeDesenho.add(teiaRender);
                }
            }
        }

        // --- INÍCIO DA ALTERAÇÃO: CAMERA CULLING (OTIMIZAÇÃO) ---

        // 1. Definimos uma margem de segurança (em pixels) para que texturas grandes
        // não desapareçam de forma brusca quando o centro delas sair da tela.
        float margemCulling = 1024f;

        // 2. Calculamos as bordas da visão atual da câmera no mundo
        float cameraEsquerda = camera.position.x - (viewport.getWorldWidth() / 2f) - margemCulling;
        float cameraDireita  = camera.position.x + (viewport.getWorldWidth() / 2f) + margemCulling;
        float cameraBaixo    = camera.position.y - (viewport.getWorldHeight() / 2f) - margemCulling;
        float cameraCima     = camera.position.y + (viewport.getWorldHeight() / 2f) + margemCulling;

        // 3. Adiciona apenas os elementos que estão dentro da visão da câmera
        for (ObjetoRenderizavel elementoMapa : elementosMapaRenderizaveis) {

            // Checagem AABB (Axis-Aligned Bounding Box) ultra rápida
            if (elementoMapa.drawX >= cameraEsquerda && elementoMapa.drawX <= cameraDireita &&
                elementoMapa.drawY >= cameraBaixo    && elementoMapa.drawY <= cameraCima) {

                listaDeDesenho.add(elementoMapa);
            }
        }

        // --- FIM DA ALTERAÇÃO ---

        listaDeDesenho.sort(zIndexComparator);

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            if (obj.tile != null) {
                obj.textura = obj.tile.getTextureRegion();
            }
            if (obj.textura != null) {
                batch.setColor(1f, 1f, 1f, obj.alpha);

                if (obj.isTransformado) {
                    batch.draw(obj.textura, obj.drawX, obj.drawY,
                        obj.originX, obj.originY,
                        obj.width, obj.height,
                        obj.scaleX, obj.scaleY, obj.grausRotacao);
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

        // --- INÍCIO DA ALTERAÇÃO DAS LUZES ---

        // 1. Pinta o SpriteBatch de Vermelho
        batch.setColor(1f, 0.2f, 0.2f, 1f);
        float raioLuzPoste = 350f;
        for (Vector2 posLuz : luzesVermelhas) {
            float lx = posLuz.x - (raioLuzPoste / 2f);
            float ly = posLuz.y - (raioLuzPoste / 2f);
            batch.draw(lightBrush, lx, ly, raioLuzPoste, raioLuzPoste);
        }

        // 2. Pinta o SpriteBatch de Azul (R=0.2, G=0.4, B=1.0)
        batch.setColor(0.2f, 0.4f, 1f, 1f);
        float raioLuzCristal = 400f; // Cristais iluminam uma área menor
        for (Vector2 posLuz : luzesAzuis) {
            float lx = posLuz.x - (raioLuzCristal / 2f);
            float ly = posLuz.y - (raioLuzCristal / 2f);
            batch.draw(lightBrush, lx, ly, raioLuzCristal, raioLuzCristal);
        }

        // 3. RETORNA a cor para Branco para a luz do jogador não bugar!
        batch.setColor(Color.WHITE);

        // --- FIM DA ALTERAÇÃO DAS LUZES ---

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

            shapeRenderer.setColor(Color.CYAN);
            for (Polygon gatilho : gatilhosMusicaBoss) {
                float[] vFisica = gatilho.getVertices();
                float[] vTela = new float[vFisica.length];

                // Transforma cada ponto físico num ponto isométrico na tela
                for (int i = 0; i < vFisica.length; i += 2) {
                    float px = vFisica[i];
                    float py = vFisica[i+1];

                    vTela[i] = (px - py) * (tile_width / 2f);
                    vTela[i+1] = -(px + py) * (tile_height / 2f);
                }

                shapeRenderer.polygon(vTela);
            }

            shapeRenderer.setColor(Color.RED);
            for (Morcego morcego : morcegos) {
                if (morcego.isAtivo) desenharRetanguloIsometrico(morcego.hitboxColisao, shapeRenderer);
            }

            shapeRenderer.setColor(Color.YELLOW);
            if (boss != null) {
                desenharRetanguloIsometrico(boss.hitbox, shapeRenderer);
            }

            shapeRenderer.setColor(Color.YELLOW);
            if (boss != null) {
                desenharRetanguloIsometrico(boss.hitbox, shapeRenderer);

                // --- DESENHA A HITBOX DAS TEIAS ---
                shapeRenderer.setColor(Color.CYAN); // Cor ciano para as teias
                for (TeiaProjetil teia : boss.teiasAtivas) {
                    desenharRetanguloIsometrico(teia.hitbox, shapeRenderer);
                }
            }

            shapeRenderer.setColor(Color.GREEN);
            desenharRetanguloIsometrico(player.hitbox, shapeRenderer);

            if (player.estaAtacando) {
                shapeRenderer.setColor(Color.BLUE);
                desenharRetanguloIsometrico(player.hitboxAtaque, shapeRenderer);
            }

            if (player.estaAtacandoPesado) {
                shapeRenderer.setColor(Color.ORANGE);
                desenharRetanguloIsometrico(player.hitboxAtaquePesado, shapeRenderer);
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

        if (boss != null && boss.isAtacando()) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 0f, 0f, 0.4f);

            for (Vector2 tile : boss.getTilesAtaqueTelegraph()) {
                Rectangle tileRect = new Rectangle(tile.x, tile.y, 1f, 1f);
                desenharTileIsometricoPreenchido(tileRect, shapeRenderer);
            }
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // --- INTERFACE DE USUÁRIO (HUD) ---
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);

        // 1. ABRE A CANETA UMA ÚNICA VEZ PARA TODA A UI
        batch.begin();

        if (pertoDoCristal && !mostrandoMensagemMagica) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "[E] Inspecionar", uiViewport.getWorldWidth() / 2f - 80f, 200f);
        }

        if (mostrandoMensagemMagica) {
            // Desenha o Fundo Escuro
            batch.setColor(1f, 1f, 1f, 0.8f);
            batch.draw(pixelPreto, 50, 50, uiViewport.getWorldWidth() - 100, 150);

            // Retorna a cor para branco ANTES de desenhar o portrait e o texto
            batch.setColor(Color.WHITE);

            batch.draw(portraitAephorul, 60, 60, 320, 320);

            font.setColor(Color.FIREBRICK);
            font.draw(batch, "Esse é apenas o início da sua jornada.", 420, 180);

            font.setColor(Color.WHITE);
            font.draw(batch, "O abismo aguarda seu retorno, herói...", 420, 120);

            font.draw(batch, "Desenvolvedores:", uiViewport.getWorldWidth() / 2, (uiViewport.getWorldHeight() / 2) + 200);
            font.draw(batch, "Matheus Dall olmo", uiViewport.getWorldWidth() / 2, (uiViewport.getWorldHeight() / 2) + 160);
            font.draw(batch, "Pablo Gabriel Sustisso ", uiViewport.getWorldWidth() / 2, (uiViewport.getWorldHeight() / 2) + 120);
            font.draw(batch, "Samuel Grontoski", uiViewport.getWorldWidth() / 2, (uiViewport.getWorldHeight() / 2) + 80);
        }

        // 2. DEBUG INFO (Removemos o begin() e end() que causavam o erro)
        if (mostrarDebugInfo) {
            String textoOverlay = String.format(
                "FPS: %d\nPOS MUNDO:\nX: %.2f | Y: %.2f\nPOS TELA:\nX: %.1f | Y: %.1f",
                Gdx.graphics.getFramesPerSecond(), player.posicaoMundo.x, player.posicaoMundo.y, screenX, screenY
            );
            font.setColor(Color.GREEN);
            font.draw(batch, textoOverlay, 15, uiViewport.getWorldHeight() - 300);
        }

        // 3. FECHA A CANETA DA UI
        batch.end();


        // --- CORTINA DE ILUMINAÇÃO (FADE IN DO JOGO) ---
        if (transicaoAlpha > 0f) {
            transicaoAlpha -= delta * VELOCIDADE_FADE;
            if (transicaoAlpha < 0f) transicaoAlpha = 0f;

            uiViewport.apply();
            batch.setProjectionMatrix(uiCamera.combined);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            // Como fechamos o batch ali em cima, podemos abrir este com segurança!
            batch.begin();
            batch.setColor(1f, 1f, 1f, transicaoAlpha);
            batch.draw(pixelPreto, 0, 0, uiViewport.getWorldWidth(), uiViewport.getWorldHeight());
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // HUD de skills
        batch.begin();

        game.batch.setProjectionMatrix(uiViewport.getCamera().combined);
        game.batch.setColor(Color.WHITE); // Garante que a UI não herde nenhuma cor de dano/luz

        // Configurações de layout fixas baseadas no tamanho virtual da sua uiViewport (640x360)
        float slotSize = 48f;
        float espacamento = 10f;
        float margemDireita = 60f;
        float margemInferior = 20f;

        // Calcula posições X da direita para a esquerda
        float xDash = uiViewport.getWorldWidth() - margemDireita - slotSize;
        float xPesado = xDash - slotSize - espacamento;
        float xLeve = xPesado - slotSize - espacamento;
        float uiY = margemInferior;

        // Calcula as porcentagens de recarga atualizadas (0.0 a 1.0)
        float porcLeve = MathUtils.clamp(player.attackTimer / player.tempoRecargaAtaque, 0f, 1f);
        float porcPesado = MathUtils.clamp(player.attackPesadoTimer / player.tempoRecargaAtaquePesado, 0f, 1f);
        float porcDash = MathUtils.clamp(player.cooldownRollTimer / player.tempoRecargaRoll, 0f, 1f);

        // Desenha os três slots com os ícones correspondentes
        desenharSlotHabilidade(uiIconAtaqueLeve, uiFrame, xLeve, uiY, slotSize, porcLeve);
        desenharSlotHabilidade(uiIconAtaquePesado, uiFrame, xPesado, uiY, slotSize, porcPesado);
        desenharSlotHabilidade(uiIconDash, uiFrame, xDash, uiY, slotSize, porcDash);

        batch.end();
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

    private void desenharTileIsometricoPreenchido(Rectangle rect, ShapeRenderer sr) {
        float x1 = rect.x, y1 = rect.y;
        float x2 = rect.x + rect.width, y2 = rect.y;
        float x3 = rect.x + rect.width, y3 = rect.y + rect.height;
        float x4 = rect.x, y4 = rect.y + rect.height;

        float sx1 = (x1 - y1) * (tile_width / 2f); float sy1 = (x1 + y1) * (tile_height / 2f);
        float sx2 = (x2 - y2) * (tile_width / 2f); float sy2 = (x2 + y2) * (tile_height / 2f);
        float sx3 = (x3 - y3) * (tile_width / 2f); float sy3 = (x3 + y3) * (tile_height / 2f);
        float sx4 = (x4 - y4) * (tile_width / 2f); float sy4 = (x4 + y4) * (tile_height / 2f);

        sr.triangle(sx1, sy1, sx2, sy2, sx3, sy3);
        sr.triangle(sx1, sy1, sx3, sy3, sx4, sy4);
    }

    private void desenharSlotHabilidade(Texture icone, Texture frame, float x, float y, float size, float porcentagemPronto) {
        // 1. Desenha o Ícone base
        game.batch.draw(icone, x, y, size, size);

        // 2. Lógica da Sombra de Cooldown (Desenhada de cima para baixo)
        // Se a porcentagem for menor que 1.0, significa que está recarregando
        if (porcentagemPronto < 1f) {
            float alturaSombra = size * (1f - porcentagemPronto); // Quanto falta para encher
            float ySombra = y + size - alturaSombra; // Posiciona a sombra na parte de cima

            game.batch.draw(uiSombraCooldown, x, ySombra, size, alturaSombra);
        }

        // 3. Desenha a Moldura (Frame) por cima de tudo para dar o acabamento limpo
        game.batch.draw(frame, x, y, size, size);
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
        pixelPreto.dispose();
        portraitAephorul.dispose();
        musicaFundo.stop();
        musicaBoss.stop();
        if (uiSombraCooldown != null) uiSombraCooldown.dispose();
    }
}
