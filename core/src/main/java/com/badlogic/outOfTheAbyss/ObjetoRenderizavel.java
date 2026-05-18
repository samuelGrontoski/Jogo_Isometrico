package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

// DTO (Data Transfer Object) de Renderização Estrutural.
// O SpriteBatch renderiza usando esta classe agnóstica sem saber o que as entidades originais são.
public class ObjetoRenderizavel {
    public TextureRegion textura;
    public float drawX;
    public float drawY;
    public float sortY; // Coordenada imperativa usada no Comparator (Painter's Algorithm)
    public float alpha = 1f;
}
