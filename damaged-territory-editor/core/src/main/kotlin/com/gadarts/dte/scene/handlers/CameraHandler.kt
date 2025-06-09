package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.dte.scene.SceneRenderer.Companion.MAP_SIZE
import com.gadarts.dte.scene.SharedData
import com.gadarts.shared.SharedUtils

class CameraHandler(private val sharedData: SharedData, dispatcher: MessageDispatcher) : InputProcessor,
    SceneHandler(dispatcher) {
    override fun update(parent: Table, deltaTime: Float) {
        sharedData.camera.update()
    }

    override fun dispose() {

    }

    init {
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
        sharedData.camera = createCamera()
    }

    private fun createCamera(): PerspectiveCamera {
        val perspectiveCamera = SharedUtils.createCamera(SharedUtils.GAME_VIEW_FOV)
        perspectiveCamera.position[9.0f, 16.0f] = 9.0f
        perspectiveCamera.direction.rotate(Vector3.X, -55.0f)
        return perspectiveCamera
    }

    private var pan: Boolean = false
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
            val position = sharedData.camera.position
            position.add(-deltaX * PAN_FACTOR, 0F, -deltaY * PAN_FACTOR)
            position.set(
                MathUtils.clamp(position.x, -MAP_EDGE_OFFSET_X, MAP_SIZE.toFloat() + MAP_EDGE_OFFSET_X),
                position.y,
                MathUtils.clamp(position.z, MAP_EDGE_OFFSET_Z, MAP_SIZE.toFloat() + MAP_EDGE_OFFSET_Z)
            )
            sharedData.camera.update()
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    companion object {
        private const val PAN_FACTOR = 0.025F
        private const val MAP_EDGE_OFFSET_X = 4F
        private const val MAP_EDGE_OFFSET_Z = 12F
    }
}
