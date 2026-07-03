package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuInicial implements Screen {
    final JogoIsometrico game;
    Stage stage;

    Texture backgroundTexture;
    Texture playButtonTexture;
    Texture exitButtonTexture;

    // Variáveis de Transição (Fade Out)
    private boolean iniciandoTransicao = false;
    private float transicaoAlpha = 0f;
    private final float VELOCIDADE_FADE = 1.0f;
    private Texture pixelPreto;

    public MenuInicial(final JogoIsometrico game) {
        this.game = game;

        if (!game.assets.isLoaded("background/tela-menu.png")) {
            game.assets.load("background/tela-menu.png", Texture.class);
            game.assets.load("botao/botao_jogar.png", Texture.class);
            game.assets.load("botao/botao_sair.png", Texture.class);
            game.assets.finishLoading(); // Trava a thread por milissegundos para concluir
        }

        stage = new Stage(new FitViewport(640, 360));

        // Obtém referências das texturas já decodificadas da RAM
        backgroundTexture = game.assets.get("background/tela-menu.png", Texture.class);
        playButtonTexture = game.assets.get("botao/botao_jogar.png", Texture.class);
        exitButtonTexture = game.assets.get("botao/botao_sair.png", Texture.class);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        pixelPreto = new Texture(pixmap);
        pixmap.dispose();

        // Estilização e instanciação do botão de Jogar
        ImageButton.ImageButtonStyle playStyle = new ImageButton.ImageButtonStyle();
        playStyle.imageUp = new TextureRegionDrawable(new TextureRegion(playButtonTexture));
        ImageButton playButton = new ImageButton(playStyle);

        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!iniciandoTransicao) {
                    game.carregarAssetsJogo();
                    iniciandoTransicao = true;
                }
            }
            // Ações interpoladas (Actions) acionadas ao entrar/sair do Hover do mouse
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

        // Estilização e instanciação do botão de Sair
        ImageButton.ImageButtonStyle exitStyle = new ImageButton.ImageButtonStyle();
        exitStyle.imageUp = new TextureRegionDrawable(new TextureRegion(exitButtonTexture));
        ImageButton exitButton = new ImageButton(exitStyle);

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
            // Ações interpoladas (Actions) acionadas ao entrar/sair do Hover do mouse
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

        // Posicionamento elástico em Tabela (similar ao uso de Grid/Flex na Web)
        Table table = new Table();
        table.setFillParent(true);
        table.left().padLeft(16);
        table.add(playButton).padBottom(16).row();
        table.add(exitButton);
        stage.addActor(table);

        // Listener Global de Tela (captura atalhos que não estão sobre Atores)
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.F11) {
                    if (Gdx.graphics.isFullscreen()) {
                        Gdx.graphics.setWindowedMode(1280, 720);
                    } else {
                        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    }
                    return true;
                }
                return super.keyDown(event, keycode);
            }
        });
    }

    @Override
    public void show() {
        // Entrega o processamento de cliques ao Stage
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

        // DESENHA A CORTINA PRETA POR CIMA DE TUDO (EFEITO FADE OUT)
        if (iniciandoTransicao) {
            // 1. Limita o tempo do frame para evitar que o engasgo do PC pule a animação
            if (delta > 0.05f) delta = 0.05f;

            transicaoAlpha += delta * VELOCIDADE_FADE;

            // 2. Garante explicitamente a ativação do Alpha (Pois o Stage pode ter desativado)
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            // 3. Desenha o fundo preto PRIMEIRO
            game.batch.begin();
            game.batch.setColor(1f, 1f, 1f, Math.min(transicaoAlpha, 1f));
            // Desenhamos gigante para garantir que frestas não fiquem visíveis caso o jogador mude a resolução
            game.batch.draw(pixelPreto, -200, -200, 2000, 2000);
            game.batch.setColor(Color.WHITE); // Limpa a cor do batch
            game.batch.end();

            // 4. SÓ MUDA DE TELA após exibir a tela totalmente preta (e com uma sobrinha de tempo para os olhos)
            if (transicaoAlpha >= 1.05f) {
                game.setScreen(new TelaCarregamento(game));
                dispose();
                return;
            }
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        pixelPreto.dispose();
    }
}
