package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.utils.Pool.Poolable;

// Efeito visual temporário (Ghost Trail) com controle de opacidade via Alpha,
// altamente otimizado por POOLING.
public class SombraDash implements Poolable {
    public ObjetoRenderizavel render;
    public float tempoDeVida;
    public final float tempo_max_vida = 0.2f;

    public SombraDash() {
        render = new ObjetoRenderizavel();
    }

    @Override
    public void reset() {
        tempoDeVida = 0f;
        render.textura = null;
        render.alpha = 1f;
    }
}
