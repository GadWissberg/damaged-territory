package com.gadarts.dte

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.SharedUtils

class CameraHandler : InputProcessor {
    fun update() {
        camera.update()
    }

    init {
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
    }

    private fun createCamera(): PerspectiveCamera {
        val perspectiveCamera = SharedUtils.createCamera(SharedUtils.GAME_VIEW_FOV)
        perspectiveCamera.position[9.0f, 16.0f] = 9.0f
        perspectiveCamera.direction.rotate(Vector3.X, -55.0f)
        return perspectiveCamera
    }

    private var pan: Boolean = false
    val camera: PerspectiveCamera = createCamera()
    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.CONTROL_LEFT) {
            pan = true
            return true
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Input.Keys.CONTROL_LEFT) {
            pan = false
            return true
        }
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
        if (pan) {
            val deltaX = Gdx.input.deltaX.toFloat()
            val deltaY = Gdx.input.deltaY.toFloat()
            camera.position.add(-deltaX * PAN_FACTOR, 0F, -deltaY * PAN_FACTOR)
            camera.update()
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    companion object {
        private const val PAN_FACTOR = 0.025F
    }
}
