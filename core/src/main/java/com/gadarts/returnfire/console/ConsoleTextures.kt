package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable

class ConsoleTextures : Disposable {
    lateinit var backgroundTexture: Texture
        private set
    var textBackgroundTexture: Texture? = null
        private set
    var cursorTexture: Texture? = null
        private set
    var arrowTexture: Texture? = null
        private set

    private fun generateTextBackgroundTexture() {
        val textBackground = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        textBackground.setColor(TEXT_BACKGROUND_COLOR)
        textBackground.fill()
        textBackgroundTexture = Texture(textBackground)
        textBackground.dispose()
    }

    private fun generateCursorTexture() {
        val cursorPixmap = Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888)
        cursorPixmap.setColor(Color.YELLOW)
        cursorPixmap.fill()
        cursorTexture = Texture(cursorPixmap)
        cursorPixmap.dispose()
    }

    private fun generateArrowTexture() {
        val arrowUpPixmap = Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888)
        arrowUpPixmap.setColor(ARROW_COLOR)
        arrowUpPixmap.fillTriangle(CURSOR_WIDTH, CURSOR_HEIGHT, CURSOR_WIDTH / 2, 0, 0, CURSOR_HEIGHT)
        arrowTexture = Texture(arrowUpPixmap)
        arrowUpPixmap.dispose()
    }

    private fun generateBackgroundTexture(height: Int): Texture {
        val pixmap = Pixmap(Gdx.graphics.width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(CONSOLE_BACKGROUND_COLOR)
        pixmap.fillRectangle(0, 0, Gdx.graphics.width, height)
        val backgroundTexture = Texture(pixmap)
        pixmap.dispose()
        return backgroundTexture
    }

    fun init(height: Int) {
        generateBackgroundTexture(height)
        generateTextBackgroundTexture()
        generateCursorTexture()
        generateArrowTexture()
    }

    override fun dispose() {
        textBackgroundTexture!!.dispose()
        backgroundTexture.dispose()
        cursorTexture!!.dispose()
        arrowTexture!!.dispose()
    }

    companion object {
        val ARROW_COLOR: Color = Color.CHARTREUSE
        const val CURSOR_WIDTH: Int = 10
        const val CURSOR_HEIGHT: Int = 10
        private val CONSOLE_BACKGROUND_COLOR = Color(0f, 0.1f, 0f, 1f)
        private val TEXT_BACKGROUND_COLOR = Color(0f, 0.2f, 0f, 0.8f)
    }
}
