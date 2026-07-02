package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ObjetoRenderizavel {
    public TextureRegion textura;
    public float drawX;
    public float drawY;
    public float sortY;
    public float alpha = 1f;
    // Transformações do Tiled Map Editor
    public boolean flipX = false;
    public boolean flipY = false;
    public int rotation = 0;
    // Metadados rígidos para Desempate de Camadas (Z-Index)
    public boolean isElementoMapa = false;
    public int zIndexMapa = 0;
}
