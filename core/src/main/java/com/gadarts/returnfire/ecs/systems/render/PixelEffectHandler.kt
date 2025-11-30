package com.gadarts.returnfire.ecs.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable

class PixelEffectHandler : Disposable {
    fun render() {
        pixelBatch.begin()
        pixelBatch.draw(
            pixelFbo.colorBufferTexture,
            0f, 0f,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat(),
            0F, 0F, 1F, 1F
        )
        pixelBatch.end()
    }

    fun begin() {
        pixelFbo.begin()
    }

    fun end() {
        pixelFbo.end()
    }

    private val pixelScale = 2
    private val pixelFbo: FrameBuffer by lazy {
        val width = Gdx.graphics.width / pixelScale
        val height = Gdx.graphics.height / pixelScale
        FrameBuffer(Pixmap.Format.RGBA8888, width, height, true).apply {
            colorBufferTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            )
        }
    }
    private val pixelBatch: SpriteBatch by lazy {
        SpriteBatch()
    }

    companion object {
        val PIXEL_SCALE = 2
    }

    override fun dispose() {
        pixelFbo.dispose()
        pixelBatch.dispose()
    }

}
