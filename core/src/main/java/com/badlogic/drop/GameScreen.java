package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Comparator;

public class GameScreen implements Screen {

    // ==========================================
    // SISTEMA E RENDERIZAÇÃO BASE (CORE)
    // ==========================================
    final JogoIsometrico game;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;

    OrthographicCamera camera;
    Viewport viewport;
    final float viewport_width = 640f;
    final float viewport_height = 360f;

    OrthographicCamera uiCamera;
    BitmapFont font;

    public static class ObjetoRenderizavel {
        public TextureRegion textura;
        public float drawX;
        public float drawY;
        public float sortY;
        public float alpha = 1f;
    }

    Array<ObjetoRenderizavel> listaDeDesenho = new Array<>();

    // ==========================================
    // MUNDO E MAPA (AMBIENTE)
    // ==========================================
    Texture mapaTexture;
    final float tile_width = 32f;
    final float tile_height = 16f;
    float limiteMapaX;
    float limiteMapaY;
    float mapaOffsetX;
    float mapaOffsetY;

    float screenX;
    float screenY;

    // ==========================================
    // JOGADOR E EFEITOS (PLAYER)
    // ==========================================
    Player player;

    class SombraDash {
        ObjetoRenderizavel render;
        float tempoDeVida;
        final float tempo_max_vida = 0.2f;
    }

    Array<SombraDash> sombrasAtivas = new Array<>();
    float tempoCriarProximaSombra = 0f;
    final float intervalo_sombras = 0.03f;

    // ==========================================
    // ENTIDADES E OBJETOS (ENTITIES)
    // ==========================================
    Array<Morcego> morcegos = new Array<>();
    Texture textureMorcegoFly;

    Array<Pedra> pedrasDoMapa = new Array<>();
    Texture pedraTexture;
    int quantidade_pedras = 50;

    public GameScreen(final JogoIsometrico game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(viewport_width, viewport_height, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.getData().setScale(0.5f);
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, viewport_width, viewport_height);

        // Configura o Mapa
        mapaTexture = new Texture("mapa/mapa_simples.png");
        limiteMapaX = 50f;
        limiteMapaY = 50f;
        mapaOffsetX = 0;
        mapaOffsetY = -limiteMapaX * (tile_height / 2f);

        // Inicializa as Pedras
        pedraTexture = new Texture("mapa/objetos/pedras/pedra_01.png");
        TextureRegion pedraRegion = new TextureRegion(pedraTexture);

        for (int i = 0; i < quantidade_pedras; i++) {
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);
            Pedra novaPedra = new Pedra(new Vector2(px, py), pedraRegion);
            pedrasDoMapa.add(novaPedra);
        }

        // Inicializa o Jogador
        Vector2 posicaoInicial = new Vector2(limiteMapaY / 2f, -limiteMapaX / 2f);
        player = new Player(posicaoInicial);

        // Inicializa os Inimigos
        textureMorcegoFly = new Texture("inimigos/morcego/morcego_fly.png");

        for (int i = 0; i < 10; i++) {
            float px = MathUtils.random(2f, limiteMapaY - 2f);
            float py = MathUtils.random(-limiteMapaX + 2f, -2f);
            morcegos.add(new Morcego(new Vector2(px, py), textureMorcegoFly));
        }
    }

    @Override
    public void render(float delta) {
        if (!input(delta)) {
            return;
        }
        logic(delta);
        draw(delta);
    }

    private boolean input(float delta) {
        player.updateInput(delta, pedrasDoMapa, limiteMapaX, limiteMapaY);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuInicial(game));
            dispose();
            return false;
        }
        return true;
    }

    private void logic(float delta) {
        player.atualizarLogicaAtaque(delta, pedrasDoMapa, morcegos);

        // Converter Coordenadas Matemáticas do Jogador para a Tela (Para a câmera seguir)
        screenX = (player.posicaoMundo.x - player.posicaoMundo.y) * (tile_width / 2f);
        screenY = (player.posicaoMundo.x + player.posicaoMundo.y) * (tile_height / 2f);

        if (player.estaDandoDash) {
            tempoCriarProximaSombra -= delta;

            if (tempoCriarProximaSombra <= 0) {
                SombraDash novaSombra = new SombraDash();
                novaSombra.render = new ObjetoRenderizavel();
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

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapaTexture, mapaOffsetX, mapaOffsetY);

        // Z-SORTING
        listaDeDesenho.clear();

        // Calcula a posição de desenho e decide qual frame mostrar
        player.atualizarRenderizacao(delta, screenX, screenY);
        player.renderObj.alpha = 1f;
        listaDeDesenho.add(player.renderObj);

        // Prepara os inimigos
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

        // Prepara as pedras
        for (Pedra pedra : pedrasDoMapa) {
            pedra.prepararZSorting(tile_width, tile_height);
            listaDeDesenho.add(pedra.renderObj);
        }

        for (SombraDash sombra : sombrasAtivas) {
            listaDeDesenho.add(sombra.render);
        }

        // Ordena tudo o que vai desenhar com base no sortY
        listaDeDesenho.sort(new Comparator<ObjetoRenderizavel>() {
            @Override
            public int compare(ObjetoRenderizavel obj1, ObjetoRenderizavel obj2) {
                return Float.compare(obj2.sortY, obj1.sortY);
            }
        });

        // Desenha
        for (ObjetoRenderizavel obj : listaDeDesenho) {
            batch.setColor(1f, 1f, 1f, obj.alpha);
            batch.draw(obj.textura, obj.drawX, obj.drawY);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // DEBUG DE COLISÃO
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Pedras (Verde)
        shapeRenderer.setColor(Color.YELLOW);
        for (Pedra pedra : pedrasDoMapa) {
            desenharRetanguloIsometrico(pedra.hitboxColisao, shapeRenderer);
        }

        // Inimigos (Amarelo)
        shapeRenderer.setColor(Color.RED);
        for (Morcego morcego : morcegos) {
            if (morcego.isAtivo) {
                desenharRetanguloIsometrico(morcego.hitboxColisao, shapeRenderer);
            }
        }

        // Jogador (Vermelho)
        shapeRenderer.setColor(Color.GREEN);
        desenharRetanguloIsometrico(player.hitbox, shapeRenderer);

        // Ataque (Azul)
        if (player.estaAtacando) {
            shapeRenderer.setColor(Color.BLUE);
            desenharRetanguloIsometrico(player.hitboxAtaque, shapeRenderer);
        }

        shapeRenderer.end();

        // Desenho da interface (UI / GPS)
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        String textoGPS = String.format("MUNDO (Grade Cartesiana): X: %.1f | Y: %.1f\n" +
                "TELA (Projecao Isom): X: %.1f | Y: %.1f\n" +
                "DIRECAO: %s",
            player.posicaoMundo.x, player.posicaoMundo.y,
            screenX, screenY, player.direcaoAtual);

        font.setColor(Color.YELLOW);
        font.draw(batch, textoGPS, 10, viewport_height - 10);

        batch.end();
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

    }
    @Override
    public void show() {}

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
        pedraTexture.dispose();
        shapeRenderer.dispose();
        player.dispose();
        font.dispose();
        for (Morcego morcego : morcegos) {
            morcego.dispose();
        }
        textureMorcegoFly.dispose();
    }
}
