package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class JogoIsometrico extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;
    public AssetManager assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(640, 360);

        // --- INÍCIO DA CRIAÇÃO DA FONTE TTF ---

        // 1. Aponta para o arquivo .ttf na sua pasta assets
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fontes/GeistPixel-Regular-VariableFont_ELSH.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        // 2. Parâmetros de customização da fonte
        parameter.size = 24; // Tamanho em pixels (ajuste para ficar bonito no seu Viewport)
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1; // Cria uma bordinha preta leve ao redor das letras para facilitar a leitura
        parameter.borderColor = Color.BLACK;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest; // Mantém o estilo "Pixel Art" crocante da sua fonte

        // 3. O gerador desenha o mapa na RAM e cria a BitmapFont pronta para uso
        font = generator.generateFont(parameter);

        // 4. Libera a fábrica da memória RAM (OBRIGATÓRIO PARA EVITAR MEMORY LEAK)
        generator.dispose();

        // --- FIM DA CRIAÇÃO DA FONTE ---

        assets = new AssetManager();
        assets.setLoader(TiledMap.class, new TmxMapLoader());

        carregarAssetsMenu();

        carregarAssetsJogo();

        this.setScreen(new MenuInicial(this));
    }

    private void carregarAssetsMenu() {
        assets.load("background/tela-menu.png", Texture.class);
        assets.load("botao/botao_jogar.png", Texture.class);
        assets.load("botao/botao_sair.png", Texture.class);
        assets.load("sons/Fantasy UI - Twilight (4).wav", Sound.class);
        assets.load("sons/The Otherside.wav", Music.class);
    }

    // Chamado pelo MenuInicial quando o botão de Jogar for clicado
    public void carregarAssetsJogo() {
        assets.load("mapa/map_cave.tmx", TiledMap.class);
        assets.load("inimigos/morcego/morcego_fly.png", Texture.class);
        assets.load("personagem/personagem_idle_se.png", Texture.class);
        assets.load("personagem/personagem_idle_sw.png", Texture.class);
        assets.load("personagem/personagem_run_se.png", Texture.class);
        assets.load("personagem/personagem_run_sw.png", Texture.class);
        assets.load("sons/Go Down.wav", Music.class);

        // Abilidades
        assets.load("skills/ataque_leve_icon.png", Texture.class);
        assets.load("skills/ataque_pesado_icon.png", Texture.class);
        assets.load("skills/dash_icon.png", Texture.class);
        assets.load("skills/frame_icon.png", Texture.class);
        assets.load("boss/Idle/Idle_SE.png", Texture.class);
        assets.load("boss/Idle/Idle_SW.png", Texture.class);
        assets.load("boss/Walk/Walk_SE.png", Texture.class);
        assets.load("boss/Walk/Walk_SW.png", Texture.class);
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
