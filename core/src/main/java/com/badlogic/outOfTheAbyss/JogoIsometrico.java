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
    public BitmapFont fontMorte;
    public FitViewport viewport;
    public AssetManager assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(640, 360);

        // --- INÍCIO DA CRIAÇÃO DAS FONTES TTF ---
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fontes/GeistPixel-Regular-VariableFont_ELSH.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 24;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1;
        parameter.borderColor = Color.BLACK;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        font = generator.generateFont(parameter);
        generator.dispose();

        // NOVO: Fonte antiquity-print
        FreeTypeFontGenerator generatorMorte = new FreeTypeFontGenerator(Gdx.files.internal("fontes/antiquity-print.ttf"));
        FreeTypeFontParameter parameterMorte = new FreeTypeFontParameter();
        parameterMorte.size = 48;
        parameterMorte.color = Color.RED;
        parameterMorte.borderWidth = 2;
        parameterMorte.borderColor = Color.BLACK;
        parameterMorte.minFilter = Texture.TextureFilter.Nearest;
        parameterMorte.magFilter = Texture.TextureFilter.Nearest;
        fontMorte = generatorMorte.generateFont(parameterMorte);
        generatorMorte.dispose();
        // --- FIM DA CRIAÇÃO DAS FONTES ---

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
        assets.load("mapa/Objetos_Cenario/parede_teia.png", Texture.class);
        assets.load("inimigos/morcego/morcego_fly.png", Texture.class);
        assets.load("personagem/personagem_idle_se.png", Texture.class);
        assets.load("personagem/personagem_idle_sw.png", Texture.class);
        assets.load("personagem/personagem_run_se.png", Texture.class);
        assets.load("personagem/personagem_run_sw.png", Texture.class);
        assets.load("sons/Go Down.wav", Music.class);
        assets.load("sons/Boss_music.mp3", Music.class);
        assets.load("sons/Boss_Die.mp3", Sound.class);
        assets.load("sons/Die.mp3", Sound.class);

        // Player
        assets.load("personagem/Idle.png", Texture.class);
        assets.load("personagem/Walk.png", Texture.class);
        assets.load("personagem/Run.png", Texture.class);
        assets.load("personagem/CrouchIdle.png", Texture.class);
        assets.load("personagem/CrouchWalk.png", Texture.class);
        assets.load("personagem/Rolling.png", Texture.class);
        assets.load("personagem/Melee1.png", Texture.class);
        assets.load("personagem/Melee2.png", Texture.class);
        assets.load("personagem/Heal.png", Texture.class);
        assets.load("personagem/Die.png", Texture.class);
        assets.load("sons/ataque_espada.wav", Sound.class);
        assets.load("sons/bater_na_porta.wav", Sound.class);
        assets.load("sons/cura.mp3", Sound.class);
        assets.load("sons/passos.wav", Sound.class);
        assets.load("personagem/Health_Bar.png", Texture.class);

        // Boss
        assets.load("boss/Idle/Idle_SE.png", Texture.class);
        assets.load("boss/Idle/Idle_SW.png", Texture.class);
        assets.load("boss/Walk/Walk_SE.png", Texture.class);
        assets.load("boss/Walk/Walk_SW.png", Texture.class);
        assets.load("boss/Attack1/Attack1_SE.png", Texture.class);
        assets.load("boss/Attack1/Attack1_SW.png", Texture.class);
        assets.load("boss/Attack2/Attack2_SE.png", Texture.class);
        assets.load("boss/Attack2/Attack2_SW.png", Texture.class);
        assets.load("boss/Proyectile/Proyectile_SE.png", Texture.class);
        assets.load("boss/Proyectile/Proyectile_SW.png", Texture.class);
        assets.load("boss/Death/Death_SE.png", Texture.class);
        assets.load("boss/Death/Death_SW.png", Texture.class);

        // Interface/Skills
        assets.load("skills/ataque_leve_icon.png", Texture.class);
        assets.load("skills/ataque_pesado_icon.png", Texture.class);
        assets.load("skills/corrida_icon.png", Texture.class);
        assets.load("skills/cura_icon.png", Texture.class);
        assets.load("skills/dash_icon.png", Texture.class);
        assets.load("skills/espaco_icon.png", Texture.class);
        assets.load("skills/frame_icon.png", Texture.class);
        assets.load("skills/mouse_dir_icon.png", Texture.class);
        assets.load("skills/mouse_esq_icon.png", Texture.class);
        assets.load("skills/shift_icon.png", Texture.class);
        assets.load("skills/tecla1_icon.png", Texture.class);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        fontMorte.dispose();
        assets.dispose();
    }
}
