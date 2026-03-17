package com.badlogic.drop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class JogoIsometrico extends ApplicationAdapter {
    private OrthographicCamera camera;
    private TiledMap map;
    private IsometricTiledMapRenderer renderer;

    private SpriteBatch batch;
    private Texture playerSprite;

    // Posição do jogador no "Mundo" (não na tela)
    private Vector2 playerMundo;
    private float velocidade = 5f;

    // Tamanho do tile do seu mapa (do Tiled)
    private final float TILE_WIDTH = 32f;
    private final float TILE_HEIGHT = 16f;

    private float limiteMapaX;
    private float limiteMapaY;

    @Override
    public void create() {
        // 1. Configurar a Câmera
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera();
        // Centraliza a câmera e define a área visível
        camera.setToOrtho(false, w, h);
        camera.position.set(0, 0, 0);
        camera.update();

        // 2. Carregar o Mapa do Tiled
        // Certifique-se de que o arquivo mapa.tmx está na pasta assets
        map = new TmxMapLoader().load("mapa_simples.tmx");

        // Pega a largura e altura em quantidade de tiles
        int mapWidth = map.getProperties().get("width", Integer.class);
        int mapHeight = map.getProperties().get("height", Integer.class);

        // Salva esses valores para usarmos na colisão
        limiteMapaX = (float) mapWidth;
        limiteMapaY = (float) mapHeight;

        // 3. Criar o Renderizador Isométrico
        renderer = new IsometricTiledMapRenderer(map);

        batch = new SpriteBatch();
        // Certifique-se de ter a imagem do personagem na pasta assets
        playerSprite = new Texture("personagem.png");

        // Nascer no meio de um mapa 50x40 (tile 25, 20)
        playerMundo = new Vector2(0, -50);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        float moveSpeed = velocidade * delta;

        // 1. Lógica de Movimento (Controles Intuitivos)
        // W / Seta para Cima (Sobe na tela)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP) || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            playerMundo.x += moveSpeed;
            playerMundo.y += moveSpeed;
        }
        // S / Seta para Baixo (Desce na tela)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN) || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) {
            playerMundo.x -= moveSpeed;
            playerMundo.y -= moveSpeed;
        }
        // D / Seta para Direita (Vai para a direita na tela)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT) || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            playerMundo.x += moveSpeed;
            playerMundo.y -= moveSpeed;
        }
        // A / Seta para Esquerda (Vai para a esquerda na tela)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT) || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            playerMundo.x -= moveSpeed;
            playerMundo.y += moveSpeed;
        }

        // 2. Colisão com as bordas do mapa
        // Trava o X entre 0 e o limite do mapa
        playerMundo.x = MathUtils.clamp(playerMundo.x, 0, limiteMapaX);

        // Trava o Y entre 0 e o limite do mapa
        playerMundo.y = MathUtils.clamp(playerMundo.y, 0, limiteMapaY);

        // 3. Converter Coordenadas do Mundo para a Tela
        float screenX = (playerMundo.x - playerMundo.y) * (TILE_WIDTH / 2f);
        float screenY = (playerMundo.x + playerMundo.y) * (TILE_HEIGHT / 2f);

        // 4. Centralizar a Câmera no Jogador
        // Opcional: Adicione um offset no Y se o personagem não ficar bem no centro da tela
        camera.position.set(screenX, screenY, 0);
        camera.update();

        // 5. Desenhar o Mapa
        renderer.setView(camera);
        renderer.render();

        // 6. Desenhar o Personagem
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Offset para centralizar o pé do sprite (64x64) no meio do tile
        float drawX = screenX - (playerSprite.getWidth() / 2f);
        float drawY = screenY; // Pode ajustar isso (ex: + 8) dependendo de onde está o pé no seu sprite

        batch.draw(playerSprite, drawX, drawY);
        batch.end();
    }

    @Override
    public void dispose() {
        map.dispose();
        batch.dispose();
        playerSprite.dispose();
        renderer.dispose();
    }
}
