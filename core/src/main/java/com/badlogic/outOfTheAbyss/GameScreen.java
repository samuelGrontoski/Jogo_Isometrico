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
import com.badlogic.gdx.utils.Align;
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
    private Texture uiIconCorrida;
    private Texture uiIconCura;
    private Texture uiIconDash;
    private Texture uiFrame;
    private Texture uiSombraCooldown;

    // Ícones dos botões de acionamento
    private Texture btnMouseEsq;
    private Texture btnMouseDir;
    private Texture btnEspaco;
    private Texture btnTecla1;
    private Texture btnShift;

    // Variáveis da Barra de Vida
    private Texture uiHealthBarSheet;
    private TextureRegion[] uiHealthBarFrames;

    // --- LUZ E FOG ---
    FrameBuffer lightBuffer;
    TextureRegion lightBufferRegion;
    Texture lightBrush;

    private final Pool<Morcego> morcegoPool = new Pool<Morcego>() {
        @Override
        protected Morcego newObject() { return new Morcego(); }
    };
    Array<Morcego> morcegos = new Array<>();
    private final Vector2[] posicoesSpawnMorcegos = new Vector2[] {
        new Vector2(30f, -49f),
        new Vector2(30f, -42f),
        new Vector2(26f, -42f),
        new Vector2(62f, -65f),
        new Vector2(59f, -52f),
        new Vector2(54f, -67f)
    };
    private final float[] timersSpawnMorcegos = new float[posicoesSpawnMorcegos.length];
    private final Morcego[] morcegosNosSpawns = new Morcego[posicoesSpawnMorcegos.length];
    private final float TEMPO_RESPAWN_MORCEGO = 60.0f; // 60 segundos
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

    // Variáveis para a transição de morte
    private boolean iniciandoMorte = false;
    private float alphaMorte = 0f;
    private Texture pixelPretoTransicao;

    // --- VARIÁVEIS DA CUTSCENE DO BOSS ---
    Polygon gatilhoEntradaBoss;
    private boolean cutsceneIniciada = false;
    private boolean cutsceneAndando = false;
    private float timerBossAcordar = 0f;
    private boolean bossAcordou = false;

    // Controles da Parede de Teia
    private Texture textureParedeTeia;
    private boolean portaFechada = false;
    private Rectangle hitboxPorta;
    private ObjetoRenderizavel portaRenderizavel;

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
        pixelPretoTransicao = new Texture(pixmap);
        pixmap.dispose();

        mapaTiled = game.assets.get("mapa/map_cave.tmx", TiledMap.class);
        textureParedeTeia = game.assets.get("mapa/Objetos_Cenario/parede_teia.png", Texture.class);

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

                    Object propEntrada = objeto.getProperties().get("entradaBoss");
                    if (propEntrada != null && (Boolean)propEntrada) {
                        PolygonMapObject polyObj = (PolygonMapObject) objeto;
                        float[] verticesTiled = polyObj.getPolygon().getTransformedVertices();
                        float[] verticesFisica = new float[verticesTiled.length];

                        // Mesma conversão de escala que você usou para o gatilho da música
                        for (int i = 0; i < verticesTiled.length; i += 2) {
                            verticesFisica[i] = verticesTiled[i + 1] / tile_height;
                            verticesFisica[i + 1] = -(verticesTiled[i] / tile_height);
                        }
                        gatilhoEntradaBoss = new Polygon(verticesFisica);
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
        Vector2 posicaoInicial = new Vector2(22f, -80f);
        player = new Player(posicaoInicial, game.assets, playerController);

        Vector2 posicaoInicialBoss = new Vector2(78f, -22f);
        boss = new Boss(posicaoInicialBoss, game.assets);

        textureMorcegoFly = game.assets.get("inimigos/morcego/morcego_fly.png", Texture.class);
        for (int i = 0; i < posicoesSpawnMorcegos.length; i++) {
            spawnarMorcegoNoIndice(i);
        }

        portraitAephorul = new Texture("portrait/dialog-portrait-Aephorul-Angry.png");

        // HUD Habilidades
        uiIconAtaqueLeve = game.assets.get("skills/ataque_leve_icon.png", Texture.class);
        uiIconAtaquePesado = game.assets.get("skills/ataque_pesado_icon.png", Texture.class);
        uiIconCorrida = game.assets.get("skills/corrida_icon.png", Texture.class);
        uiIconCura = game.assets.get("skills/cura_icon.png", Texture.class);
        uiIconDash = game.assets.get("skills/dash_icon.png", Texture.class);
        uiFrame = game.assets.get("skills/frame_icon.png", Texture.class);
        // --- ÍCONES DOS BOTÕES ---
        btnMouseEsq = game.assets.get("skills/mouse_esq_icon.png", Texture.class);
        btnMouseDir = game.assets.get("skills/mouse_dir_icon.png", Texture.class);
        btnEspaco = game.assets.get("skills/espaco_icon.png", Texture.class);
        btnTecla1 = game.assets.get("skills/tecla1_icon.png", Texture.class);
        btnShift = game.assets.get("skills/shift_icon.png", Texture.class);

        // Inicialização e fatiamento da Barra de Vida
        uiHealthBarSheet = game.assets.get("personagem/Health_Bar.png", Texture.class);
        uiHealthBarFrames = new TextureRegion[6];

        // Divide a textura inteira por 6 para descobrir a largura exata de 1 frame
        int frameWidth = uiHealthBarSheet.getWidth() / 6;
        int frameHeight = uiHealthBarSheet.getHeight();

        for (int i = 0; i < 6; i++) {
            uiHealthBarFrames[i] = new TextureRegion(uiHealthBarSheet, i * frameWidth, 0, frameWidth, frameHeight);
        }

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

        if (player.isDead) {
            // Se morreu, dispara a transição apenas uma vez
            if (!iniciandoMorte) {
                iniciandoMorte = true;
            }
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

        // --- CORREÇÃO AQUI ---
        // Extraímos a hitbox do boss de forma segura (se o boss for null, passamos null)
        Rectangle hitboxDoBoss = (boss != null) ? boss.hitbox : null;

        // Chamada atualizada respeitando a nova ordem de parâmetros na classe Player
        player.updateInput(delta, hitboxesMapa, hitboxDoBoss, limiteMapaX, limiteMapaY, auxMousePos.x, auxMousePos.y);

        return true;
    }

    private void logic(float delta) {
        // LÓGICA DA CUTSCENE DO BOSS ---

        // 1. Verifica se o player pisou na entrada
        if (!cutsceneIniciada && gatilhoEntradaBoss != null) {
            float centroX = player.hitbox.x + (player.hitbox.width / 2f);
            float centroY = player.hitbox.y + (player.hitbox.height / 2f);

            if (gatilhoEntradaBoss.contains(centroX, centroY)) {
                cutsceneIniciada = true;
                cutsceneAndando = true;
                player.emCutscene = true;
                // Define o ponto alvo que você pediu
                player.destinoCutscene.set(66f, -31f);
            }
        }

        // 2. Controla o andamento do player e o timer
        if (cutsceneAndando) {
            // Se o player chegou muito perto do destino (0.3f de tolerância)
            if (player.posicaoMundo.dst(player.destinoCutscene) <= 0.3f) {
                cutsceneAndando = false;
                player.emCutscene = false; // Devolve os controles ao jogador

                // Opcional: Faz o jogador olhar para noroeste (onde o boss está) ao chegar no ponto
                player.direcaoAtual = "NW";

                timerBossAcordar = 1.5f; // Dispara o cronômetro do boss
            }
        } else if (cutsceneIniciada && !bossAcordou) {
            timerBossAcordar -= delta;
            if (timerBossAcordar <= 0f) {
                bossAcordou = true;
                if (boss != null) boss.isAtivo = true;
                fecharPortaBoss();
            }
        }
        // --- FIM DA CUTSCENE ---

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

        // --- AQUI ESTÁ A NOVA CONDIÇÃO ---
        // Se o boss já morreu, forçamos o sistema a achar que a sala do boss "acabou",
        // fazendo o crossfade retornar para a música normal.
        if (boss != null && boss.isDead) {
            naSalaDoBoss = false;

            if (portaFechada) {
                portaFechada = false;
                hitboxesMapa.removeValue(hitboxPorta, true);
                elementosMapaRenderizaveis.removeValue(portaRenderizavel, true);
            }
        }

        float velocidadeFade = 1.0f * delta; // Velocidade da transição (1 segundo para trocar)

        if (naSalaDoBoss) {
            if (!musicaBoss.isPlaying()) musicaBoss.play();
            // Aumenta o Boss, diminui a Caverna (Limitado em 0.1f conforme o seu original)
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

        player.atualizarLogicaAtaque(delta, morcegos, boss);

        if (boss != null) {
            boss.update(delta, player, hitboxesMapa, limitesLayer.getHeight(), limitesLayer.getWidth());

            for (TeiaProjetil teia : boss.teiasAtivas) {
                if (teia.hitbox.overlaps(player.hitbox)) {

                    if (teia.voando) {
                        teia.voando = false;
                        teia.stateTime = 0f;
                        teia.posicaoMundo.set(player.posicaoMundo);
                        teia.hitbox.setPosition(teia.posicaoMundo.x, teia.posicaoMundo.y);
                        player.tomarDano(1);
                    }
                }
            }
        }

        screenX = (player.posicaoMundo.x - player.posicaoMundo.y) * (tile_width / 2f);
        screenY = (player.posicaoMundo.x + player.posicaoMundo.y) * (tile_height / 2f);

        // --- LÓGICA DE RESPAWN DOS MORCEGOS (60 segundos) ---
        for (int i = 0; i < posicoesSpawnMorcegos.length; i++) {
            Morcego m = morcegosNosSpawns[i];

            // Se não há morcego neste spawn, ou se o morcego que estava lá morreu (isAtivo = false)
            if (m == null || !m.isAtivo) {
                // Limpa a referência se ele acabou de morrer
                morcegosNosSpawns[i] = null;

                // Conta o tempo
                timersSpawnMorcegos[i] += delta;

                // Revive o morcego ao chegar em 60 segundos
                if (timersSpawnMorcegos[i] >= TEMPO_RESPAWN_MORCEGO) {
                    spawnarMorcegoNoIndice(i);
                }
            }
        }

        // --- ATUALIZAÇÃO E REMOÇÃO DOS MORCEGOS (Mantém o Object Pooling) ---
        for (int i = morcegos.size - 1; i >= 0; i--) {
            Morcego morcego = morcegos.get(i);

            if (!morcego.isAtivo) {
                morcegos.removeIndex(i);
                morcegoPool.free(morcego); // Devolve para a Pool limpinho!
                continue;
            }

            morcego.update(delta, player.posicaoMundo, morcegos, limiteMapaX, limiteMapaY);

            // --- APLICA O DANO AO PLAYER ---
            if (morcego.hitboxColisao.overlaps(player.hitbox)) {
                // Só bate se o cooldown do morcego já recarregou
                if (morcego.attackCooldown >= morcego.tempo_recarga_ataque) {
                    player.tomarDano(1);
                    morcego.attackCooldown = 0f; // Reseta o ataque DESSSE morcego
                }
            }
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
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        listaDeDesenho.clear();
        player.atualizarRenderizacao(delta, screenX, screenY);
        player.renderObj.alpha = 1f;
        // Opcional: Garantir que o player não receba cor residual
        player.renderObj.color = Color.WHITE;
        listaDeDesenho.add(player.renderObj);

        for (Morcego morcego : morcegos) {
            float mScreenX = (morcego.posicaoMundo.x - morcego.posicaoMundo.y) * (tile_width / 2f);
            float mScreenY = (morcego.posicaoMundo.x + morcego.posicaoMundo.y) * (tile_height / 2f);
            morcego.prepararZSorting(mScreenX, mScreenY);
            morcego.renderObj.alpha = 1f;
            morcego.renderObj.color = Color.WHITE;
            listaDeDesenho.add(morcego.renderObj);
        }

        if (boss != null) {
            TextureRegion frame = boss.getCurrentFrame();
            if (frame != null) {
                ObjetoRenderizavel bossRender = new ObjetoRenderizavel();
                bossRender.textura = frame;
                bossRender.drawX = boss.getScreenX() - (frame.getRegionWidth() / 2f);
                bossRender.drawY = boss.getScreenY() - 175f;
                bossRender.sortY = boss.getScreenY();
                bossRender.alpha = 1f;

                // --- AQUI ---
                // Copiamos a cor exata que está sendo calculada pela classe Boss
                bossRender.color = boss.renderObj.color != null ? boss.renderObj.color : Color.WHITE;

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
                    teiaRender.drawY = tScreenY - 0f;
                    teiaRender.sortY = tScreenY;
                    teiaRender.alpha = 1f;
                    teiaRender.color = Color.WHITE;
                    teiaRender.isElementoMapa = false;
                    listaDeDesenho.add(teiaRender);
                }
            }
        }

        // --- CULLING ---
        float margemCulling = 1024f;
        float cameraEsquerda = camera.position.x - (viewport.getWorldWidth() / 2f) - margemCulling;
        float cameraDireita  = camera.position.x + (viewport.getWorldWidth() / 2f) + margemCulling;
        float cameraBaixo    = camera.position.y - (viewport.getWorldHeight() / 2f) - margemCulling;
        float cameraCima     = camera.position.y + (viewport.getWorldHeight() / 2f) + margemCulling;

        for (ObjetoRenderizavel elementoMapa : elementosMapaRenderizaveis) {
            if (elementoMapa.drawX >= cameraEsquerda && elementoMapa.drawX <= cameraDireita &&
                elementoMapa.drawY >= cameraBaixo    && elementoMapa.drawY <= cameraCima) {
                listaDeDesenho.add(elementoMapa);
            }
        }

        listaDeDesenho.sort(zIndexComparator);

        for (ObjetoRenderizavel obj : listaDeDesenho) {
            if (obj.tile != null) {
                obj.textura = obj.tile.getTextureRegion();
            }
            if (obj.textura != null) {

                // --- AQUI ---
                // Aplicamos a cor da entidade ao SpriteBatch.
                // A cor contém valores normalizados R, G, B e o Alpha.
                if (obj.color != null) {
                    batch.setColor(obj.color.r, obj.color.g, obj.color.b, obj.alpha);
                } else {
                    batch.setColor(1f, 1f, 1f, obj.alpha);
                }

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

        // O restante do método (luzes, hitboxes, HUD, etc) continua inalterado daqui para baixo...

        lightBuffer.begin();
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 0.95f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        batch.setColor(1f, 0.2f, 0.2f, 1f);
        float raioLuzPoste = 350f;
        for (Vector2 posLuz : luzesVermelhas) {
            float lx = posLuz.x - (raioLuzPoste / 2f);
            float ly = posLuz.y - (raioLuzPoste / 2f);
            batch.draw(lightBrush, lx, ly, raioLuzPoste, raioLuzPoste);
        }

        batch.setColor(0.2f, 0.4f, 1f, 1f);
        float raioLuzCristal = 400f;
        for (Vector2 posLuz : luzesAzuis) {
            float lx = posLuz.x - (raioLuzCristal / 2f);
            float ly = posLuz.y - (raioLuzCristal / 2f);
            batch.draw(lightBrush, lx, ly, raioLuzCristal, raioLuzCristal);
        }

        batch.setColor(Color.WHITE);

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

                shapeRenderer.setColor(Color.CYAN);
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

            if (boss != null && !boss.isDead) {
                float bScreenX = boss.getScreenX();
                float bScreenY = boss.getScreenY();

                float larguraBarraBoss = 60f;
                float alturaBarraBoss = 6f;
                float barraBossX = bScreenX - (larguraBarraBoss / 2f);
                float barraBossY = bScreenY + 40f;

                shapeRenderer.setColor(Color.RED);
                shapeRenderer.rect(barraBossX, barraBossY, larguraBarraBoss, alturaBarraBoss);

                shapeRenderer.setColor(Color.GREEN);
                float proporcaoVidaBoss = Math.max(0, (float) boss.vida / boss.vidaMaxima);
                shapeRenderer.rect(barraBossX, barraBossY, larguraBarraBoss * proporcaoVidaBoss, alturaBarraBoss);
            }

            if (!player.isDead) {
                float pScreenX = (player.posicaoMundo.x - player.posicaoMundo.y) * (tile_width / 2f);
                float pScreenY = (player.posicaoMundo.x + player.posicaoMundo.y) * (tile_height / 2f);

                float larguraBarraPlayer = 40f;
                float alturaBarraPlayer = 5f;
                float barraPlayerX = pScreenX - (larguraBarraPlayer / 2f);
                float barraPlayerY = pScreenY + 40f; // Ajuste acima da cabeça

                shapeRenderer.setColor(Color.RED);
                shapeRenderer.rect(barraPlayerX, barraPlayerY, larguraBarraPlayer, alturaBarraPlayer);

                shapeRenderer.setColor(Color.GREEN);
                float proporcaoVidaPlayer = Math.max(0, (float) player.vida / player.vidaMaxima);
                shapeRenderer.rect(barraPlayerX, barraPlayerY, larguraBarraPlayer * proporcaoVidaPlayer, alturaBarraPlayer);
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

        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        if (pertoDoCristal && !mostrandoMensagemMagica) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "[E] Inspecionar", uiViewport.getWorldWidth() / 2f - 80f, 200f);
        }

        if (mostrandoMensagemMagica) {
            batch.setColor(1f, 1f, 1f, 0.8f);
            batch.draw(pixelPreto, 50, 50, uiViewport.getWorldWidth() - 100, 150);
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

        if (mostrarDebugInfo) {
            String textoOverlay = String.format(
                "FPS: %d\nPOS MUNDO:\nX: %.2f | Y: %.2f\nPOS TELA:\nX: %.1f | Y: %.1f",
                Gdx.graphics.getFramesPerSecond(), player.posicaoMundo.x, player.posicaoMundo.y, screenX, screenY
            );
            font.setColor(Color.GREEN);
            font.draw(batch, textoOverlay, 15, uiViewport.getWorldHeight() - 300);
        }
        batch.end();

        if (transicaoAlpha > 0f) {
            transicaoAlpha -= delta * VELOCIDADE_FADE;
            if (transicaoAlpha < 0f) transicaoAlpha = 0f;

            uiViewport.apply();
            batch.setProjectionMatrix(uiCamera.combined);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            batch.begin();
            batch.setColor(1f, 1f, 1f, transicaoAlpha);
            batch.draw(pixelPreto, 0, 0, uiViewport.getWorldWidth(), uiViewport.getWorldHeight());
            batch.setColor(Color.WHITE);
            batch.end();
        }

        batch.begin();
        game.batch.setProjectionMatrix(uiViewport.getCamera().combined);
        game.batch.setColor(Color.WHITE);

        float slotSize = 60f;
        float espacamento = 10f;
        float margemDireita = 60f;
        float margemInferior = 20f;
        float margemEsquerda = 60f;

        int indexVida = MathUtils.clamp(player.vida, 0, 5);
        TextureRegion currentHealthFrame = uiHealthBarFrames[indexVida];

        float escalaBarra = 4f;
        float barraWidth = currentHealthFrame.getRegionWidth() * escalaBarra;
        float barraHeight = currentHealthFrame.getRegionHeight() * escalaBarra;

        game.batch.draw(currentHealthFrame, margemEsquerda, margemInferior, barraWidth, barraHeight);

        // Calcula posições X da direita para a esquerda
        float xCorrida = uiViewport.getWorldWidth() - margemDireita - slotSize;
        float xCura = xCorrida - slotSize - espacamento;
        float xRoll = xCura - slotSize - espacamento;
        float xPesado = xRoll - slotSize - espacamento;
        float xLeve = xPesado - slotSize - espacamento;
        float uiY = margemInferior;

        // Calcula as porcentagens de recarga atualizadas (0.0 a 1.0)
        float porcCura = (player.curasAtuais > 0) ? MathUtils.clamp(player.cooldownCuraTimer / player.tempoRecargaCura, 0f, 1f) : 0f;
        float porcLeve = MathUtils.clamp(player.attackTimer / player.tempoRecargaAtaque, 0f, 1f);
        float porcPesado = MathUtils.clamp(player.attackPesadoTimer / player.tempoRecargaAtaquePesado, 0f, 1f);
        float porcRoll = MathUtils.clamp(player.cooldownRollTimer / player.tempoRecargaRoll, 0f, 1f);

        // Desenha os slots com os ícones correspondentes
        desenharSlotHabilidade(uiIconAtaqueLeve, uiFrame, xLeve, uiY, slotSize, porcLeve);
        desenharSlotHabilidade(uiIconAtaquePesado, uiFrame, xPesado, uiY, slotSize, porcPesado);
        desenharSlotHabilidade(uiIconDash, uiFrame, xRoll, uiY, slotSize, porcRoll);
        desenharSlotHabilidade(uiIconCura, uiFrame, xCura, uiY, slotSize, porcCura);
        desenharSlotHabilidade(uiIconCorrida, uiFrame, xCorrida, uiY, slotSize, 1f);

        // --- DESENHA O CONTADOR DE CURAS ---
        // Usamos a fonte já existente para escrever sutilmente no canto inferior direito do ícone
        font.getData().setScale(1.2f); // tamanho da fonte
        font.setColor(Color.WHITE);

        // A string que será desenhada (ex: "2", "1" ou "0")
        String textoCura = String.valueOf(player.curasAtuais);
        float textX = xCura + slotSize - 20f;
        float textY = uiY + 24f;
        font.draw(game.batch, textoCura, textX, textY);

        // Restaura a escala e cor para não afetar outras coisas que usem a fonte
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        // Tamanho padrão dos botões quadrados (Q, Mouse, etc)
        float btnSize = 24f;

        // Se a sua imagem do espaço ou do shift for retangular (mais comprida),
        // você pode ajustar essas larguras específicas abaixo:
        float btnWidthShift = 36f;
        float btnWidthEspaco = 40f;

        // Distância vertical: Começa onde a skill está (uiY) + a altura da skill (slotSize) + uma margem de 5 pixels
        float btnY = uiY + slotSize + 5f;

        // Desenha Mouse Esquerdo (Ataque Leve)
        float xBtnEsq = xLeve + (slotSize / 2f) - (btnSize / 2f);
        game.batch.draw(btnMouseEsq, xBtnEsq, btnY, btnSize, btnSize);

        // Desenha Mouse Direito (Ataque Pesado)
        float xBtnDir = xPesado + (slotSize / 2f) - (btnSize / 2f);
        game.batch.draw(btnMouseDir, xBtnDir, btnY, btnSize, btnSize);

        // Desenha Espaço (Dash) - Usando a largura ajustável
        float xBtnEspaco = xRoll + (slotSize / 2f) - (btnWidthEspaco / 2f);
        game.batch.draw(btnEspaco, xBtnEspaco, btnY, btnWidthEspaco, btnSize);

        // Desenha Tecla 1 (Cura)
        float xBtn1 = xCura + (slotSize / 2f) - (btnSize / 2f);
        game.batch.draw(btnTecla1, xBtn1, btnY, btnSize, btnSize);

        // Desenha Shift (Corrida) - Usando a largura ajustável
        float xBtnShift = xCorrida + (slotSize / 2f) - (btnWidthShift / 2f);
        game.batch.draw(btnShift, xBtnShift, btnY, btnWidthShift, btnSize);

        // Efeito de Fade Out ao morrer
        if (iniciandoMorte) {

            // Só começa a escurecer a tela SE a animação do player terminou
            if (player.isAnimacaoMorteTerminada()) {

                alphaMorte += delta * 1.0f;

                // O SpriteBatch JÁ ESTÁ ABERTO aqui! Não precisamos chamar begin() de novo.
                // A transparência (Blend) também já é nativa do SpriteBatch.

                // Apenas tingimos o batch de preto com Alpha e desenhamos o retângulo
                game.batch.setColor(0f, 0f, 0f, Math.min(alphaMorte, 1f));
                game.batch.draw(pixelPretoTransicao, 0, 0, uiViewport.getWorldWidth(), uiViewport.getWorldHeight());

                // Restaura a cor do batch para branco para não afetar os frames seguintes
                game.batch.setColor(Color.WHITE);

                // Quando a tela estiver 100% preta, muda de tela
                if (alphaMorte >= 1.05f) {
                    game.setScreen(new TelaMorte(game));
                    dispose();
                }
            }
        }

        batch.end();
    }

    private void spawnarMorcegoNoIndice(int index) {
        // Puxa da sua Pool (reaproveitamento de memória)
        Morcego m = morcegoPool.obtain();

        // Inicia o morcego na coordenada específica do índice
        m.init(new Vector2(posicoesSpawnMorcegos[index].x, posicoesSpawnMorcegos[index].y), textureMorcegoFly);

        // Adiciona na lista principal de renderização/lógica
        morcegos.add(m);

        // Salva a referência para sabermos que este ponto de spawn está ocupado
        morcegosNosSpawns[index] = m;
        timersSpawnMorcegos[index] = 0f; // Reseta o cronômetro
    }

    private void fecharPortaBoss() {
        if (gatilhoEntradaBoss == null) return;

        portaFechada = true;

        // 1. Cria a barreira física invisível baseada no polígono da entrada
        Rectangle bounds = gatilhoEntradaBoss.getBoundingRectangle();
        hitboxPorta = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        // Adiciona na lista geral de colisões (bloqueia o Player e o Boss de sair)
        hitboxesMapa.add(hitboxPorta);

        // 2. Cria a arte visual da teia para participar do Z-Sorting (Profundidade isométrica)
        portaRenderizavel = new ObjetoRenderizavel();
        portaRenderizavel.textura = new TextureRegion(textureParedeTeia);

        // Encontra o centro matemático da entrada
        float centroMundoX = bounds.x + (bounds.width / 2f);
        float centroMundoY = bounds.y + (bounds.height / 2f);

        // Converte para as coordenadas de tela Isométrica
        float pScreenX = (centroMundoX - centroMundoY) * (tile_width / 2f);
        float pScreenY = (centroMundoX + centroMundoY) * (tile_height / 2f);

        // Centraliza a imagem da teia no eixo X
        portaRenderizavel.drawX = pScreenX - (portaRenderizavel.textura.getRegionWidth() / 2f);
        // O eixo Y pode precisar de um pequeno ajuste dependendo de quão alta é a sua imagem da teia
        portaRenderizavel.drawY = pScreenY;

        // Define a ordem de desenho para renderizar corretamente atrás ou na frente do player
        portaRenderizavel.sortY = pScreenY;

        portaRenderizavel.isElementoMapa = false;
        portaRenderizavel.alpha = 1f;
        portaRenderizavel.color = Color.WHITE;

        // Adiciona ao cenário. O jogo passará a desenhar essa teia automaticamente!
        elementosMapaRenderizaveis.add(portaRenderizavel);
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
        pixelPretoTransicao.dispose();
        portraitAephorul.dispose();
        musicaFundo.stop();
        musicaBoss.stop();
        if (uiSombraCooldown != null) uiSombraCooldown.dispose();
    }
}
