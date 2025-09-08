package com.gadarts.returnfire.ecs.systems.hud

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.gadarts.returnfire.ecs.systems.data.GameSessionDataGameplay
import com.gadarts.returnfire.ecs.systems.data.SessionState
import com.gadarts.returnfire.ecs.systems.data.hud.Minimap

class HudInputProcessor(private val minimap: Minimap, private val gameSessionDataGameplay: GameSessionDataGameplay) :
    InputProcessor {

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.TAB) {
            minimap.isVisible = !minimap.isVisible
            gameSessionDataGameplay.sessionState = if (minimap.isVisible) SessionState.PAUSED else SessionState.PLAYING
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
