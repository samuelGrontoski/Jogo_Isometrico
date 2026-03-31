package com.badlogic.drop;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.drop.GameScreen.ObjetoRenderizavel;

public class Pedra {

    public Vector2 posicaoMundo;
    public Rectangle hitboxColisao;
    public ObjetoRenderizavel renderObj;

    public Pedra(Vector2 posicaoInicial, TextureRegion texturaPedra) {
        this.posicaoMundo = posicaoInicial;
        this.hitboxColisao = new Rectangle(posicaoInicial.x, posicaoInicial.y, 1f, 1f);
        this.renderObj = new ObjetoRenderizavel();
        this.renderObj.textura = texturaPedra;
        this.renderObj.alpha = 1f;
    }

    public void prepararZSorting(float tileWidth, float tileHeight) {
        float pScreenX = (posicaoMundo.x - posicaoMundo.y) * (tileWidth / 2f);
        float pScreenY = (posicaoMundo.x + posicaoMundo.y) * (tileHeight / 2f);
        this.renderObj.sortY = pScreenY;
        this.renderObj.drawX = pScreenX - (renderObj.textura.getRegionWidth() / 2f);
        this.renderObj.drawY = pScreenY;
    }
}
