package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
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

        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        assets = new AssetManager();
        assets.setLoader(TiledMap.class, new TmxMapLoader());

        // 1. Enfileira apenas os recursos da tela de Menu
        carregarAssetsMenu();

        // 2. Trava a thread da aplicação milissegundos para carregar os assets do Menu instantaneamente
        assets.finishLoading();

        // 3. Inicia o jogo diretamente no Menu, sem passar pelo Loading
        this.setScreen(new MenuInicial(this));
    }

    private void carregarAssetsMenu() {
        assets.load("background/tela-menu.png", Texture.class);
        assets.load("botao/botao_jogar.png", Texture.class);
        assets.load("botao/botao_sair.png", Texture.class);
    }

    // Chamado pelo MenuInicial quando o botão de Jogar for clicado
    public void carregarAssetsJogo() {
        assets.load("mapa/map_cave.tmx", TiledMap.class);
        assets.load("inimigos/morcego/morcego_fly.png", Texture.class);
        assets.load("personagem/personagem_idle_se.png", Texture.class);
        assets.load("personagem/personagem_idle_sw.png", Texture.class);
        assets.load("personagem/personagem_run_se.png", Texture.class);
        assets.load("personagem/personagem_run_sw.png", Texture.class);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        assets.dispose();
    }
}
