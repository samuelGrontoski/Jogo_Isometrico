package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;

public class ObjetoRenderizavel {
    public TextureRegion textura;
    public float drawX;
    public float drawY;
    public float sortY;
    public float alpha = 1f;

    // Propriedades para transformações (Escala, Rotação)
    public boolean isTransformado = false;
    public float width;
    public float height;
    public float originX;
    public float originY;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float grausRotacao = 0f;

    // Propriedades exclusivas do Mapa Tiled
    public boolean isElementoMapa = false;
    public int zIndexMapa = 0;
    public TiledMapTile tile;
    public boolean flipX = false;
    public boolean flipY = false;
    public int rotation = 0;
}
