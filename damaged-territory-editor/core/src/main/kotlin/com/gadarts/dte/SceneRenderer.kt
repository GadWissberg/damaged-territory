package com.gadarts.dte

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable

class SceneRenderer : Table(), InputProcessor, Disposable {
    private val auxiliaryModels = AuxiliaryModels()
    private val handlers = SceneRendererHandlers(auxiliaryModels)

    fun render() {
        handlers.cameraHandler.update()
        val screenPosition = localToScreenCoordinates(auxVector2.set(0F, 0F))
        handlers.renderingHandler.render(screenPosition)
    }


    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }


    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun dispose() {
        handlers.dispose()
        auxiliaryModels.dispose()
    }


    companion object {
        private val auxVector2 = com.badlogic.gdx.math.Vector2()
    }
}
