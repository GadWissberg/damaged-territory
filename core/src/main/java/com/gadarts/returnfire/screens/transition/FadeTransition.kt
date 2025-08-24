package com.gadarts.returnfire.screens.transition

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class FadeTransition(override val duration: Float) : ScreenTransition {
    private val shapeRenderer = ShapeRenderer()

    override fun render(alpha: Float, deltaTime: Float) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, alpha)
        shapeRenderer.rect(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

}
