package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class JogoIsometrico extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;
    public AssetManager assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new FitViewport(640, 360);

        // Desativa posições inteiras para garantir renderização de texto sub-pixel mais suave
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        assets = new AssetManager();
        // Enfileira os arquivos na RAM, mas NÃO bloqueia a execução
        carregarAssets();

        // Delega o controle inicial para a tela de loading assíncrono
        this.setScreen(new TelaCarregamento(this));
    }

    // Enfileiramento das texturas que o AssetManager buscará
    private void carregarAssets() {
        assets.load("background/tela-menu.png", Texture.class);
        assets.load("botao/botao_jogar.png", Texture.class);
        assets.load("botao/botao_sair.png", Texture.class);
        assets.load("mapa/mapa_simples.png", Texture.class);
        assets.load("mapa/objetos/pedras/pedra_01.png", Texture.class);
        assets.load("inimigos/morcego/morcego_fly.png", Texture.class);
        assets.load("personagem/personagem_idle_se.png", Texture.class);
        assets.load("personagem/personagem_idle_sw.png", Texture.class);
        assets.load("personagem/personagem_run_se.png", Texture.class);
        assets.load("personagem/personagem_run_sw.png", Texture.class);
    }

    // Delega a rotina de render() para a Screen ativa
    @Override
    public void render() {
        super.render();
    }

    // Libera os recursos estáticos globais no encerramento da aplicação
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        assets.dispose();
    }
}
