package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable

class ConsoleTextures(private val height: Int) : Disposable {
    val backgroundTexture: Texture by lazy { generateBackgroundTexture() }
    val textBackgroundTexture: Texture by lazy { generateTextBackgroundTexture() }
    val cursorTexture: Texture by lazy { generateCursorTexture() }
    val arrowTexture: Texture by lazy { generateArrowTexture() }

    private fun generateTextBackgroundTexture(): Texture {
        val textBackground = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        textBackground.setColor(TEXT_BACKGROUND_COLOR)
        textBackground.fill()
        val textBackgroundTexture = Texture(textBackground)
        textBackground.dispose()
        return textBackgroundTexture
    }

    private fun generateCursorTexture(): Texture {
        val cursorPixmap = Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888)
        cursorPixmap.setColor(Color.YELLOW)
        cursorPixmap.fill()
        val cursorTexture = Texture(cursorPixmap)
        cursorPixmap.dispose()
        return cursorTexture
    }

    private fun generateArrowTexture(): Texture {
        val arrowUpPixmap = Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888)
        arrowUpPixmap.setColor(ARROW_COLOR)
        arrowUpPixmap.fillTriangle(CURSOR_WIDTH, CURSOR_HEIGHT, CURSOR_WIDTH / 2, 0, 0, CURSOR_HEIGHT)
        val arrowTexture = Texture(arrowUpPixmap)
        arrowUpPixmap.dispose()
        return arrowTexture
    }

    private fun generateBackgroundTexture(): Texture {
        val pixmap = Pixmap(Gdx.graphics.width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(CONSOLE_BACKGROUND_COLOR)
        pixmap.fillRectangle(0, 0, Gdx.graphics.width, height)
        val backgroundTexture = Texture(pixmap)
        pixmap.dispose()
        return backgroundTexture
    }

    override fun dispose() {
        textBackgroundTexture.dispose()
        backgroundTexture.dispose()
        cursorTexture.dispose()
        arrowTexture.dispose()
    }

    companion object {
        val ARROW_COLOR: Color = Color.CHARTREUSE
        const val CURSOR_WIDTH: Int = 10
        const val CURSOR_HEIGHT: Int = 10
        private val CONSOLE_BACKGROUND_COLOR = Color(0f, 0.1f, 0f, 1f)
        private val TEXT_BACKGROUND_COLOR = Color(0f, 0.2f, 0f, 0.8f)
    }
}
