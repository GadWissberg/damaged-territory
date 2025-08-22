package com.gadarts.returnfire.ecs.systems.hud

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.gadarts.returnfire.ecs.systems.data.GameSessionData

class HudInputProcessor(private val gameSessionData: GameSessionData) : InputProcessor {

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.TAB) {
            val minimap = gameSessionData.hudData.minimap
            minimap.isVisible = !minimap.isVisible
            return true
        }
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


}
