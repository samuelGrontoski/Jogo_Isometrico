package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent; // Import NOVO
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions; // Import NOVO
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener; // Import NOVO (Substituiu o ChangeListener)
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuInicial implements Screen {
    final JogoIsometrico game;
    Stage stage;
    Skin skin;
    Texture backgroundTexture;
    Texture playButtonTexture;
    Texture exitButtonTexture;

    public MenuInicial(final JogoIsometrico game) {
        this.game = game;

        stage = new Stage(new FitViewport(640, 360));

        backgroundTexture = new Texture("background/tela-menu.png");
        playButtonTexture = new Texture("botao/botao_jogar.png");
        exitButtonTexture = new Texture("botao/botao_sair.png");

        // Botão Jogar
        ImageButton.ImageButtonStyle playStyle = new ImageButton.ImageButtonStyle();
        playStyle.imageUp = new TextureRegionDrawable(new TextureRegion(playButtonTexture));
        ImageButton playButton = new ImageButton(playStyle);

        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
                dispose();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    playButton.getImage().clearActions();
                    playButton.getImage().addAction(Actions.moveTo(10f, 0f, 0.15f));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (pointer == -1) {
                    playButton.getImage().clearActions();
                    playButton.getImage().addAction(Actions.moveTo(0f, 0f, 0.15f));
                }
            }
        });

        // Botão Sair
        ImageButton.ImageButtonStyle exitStyle = new ImageButton.ImageButtonStyle();
        exitStyle.imageUp = new TextureRegionDrawable(new TextureRegion(exitButtonTexture));
        ImageButton exitButton = new ImageButton(exitStyle);

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    exitButton.getImage().clearActions();
                    exitButton.getImage().addAction(Actions.moveTo(10f, 0f, 0.15f));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (pointer == -1) {
                    exitButton.getImage().clearActions();
                    exitButton.getImage().addAction(Actions.moveTo(0f, 0f, 0.15f));
                }
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.left().padLeft(16);
        table.add(playButton).padBottom(16).row();
        table.add(exitButton);
        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply();
        game.batch.setProjectionMatrix(stage.getCamera().combined);

        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, 640, 360);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        playButtonTexture.dispose();
        exitButtonTexture.dispose();
    }
}
