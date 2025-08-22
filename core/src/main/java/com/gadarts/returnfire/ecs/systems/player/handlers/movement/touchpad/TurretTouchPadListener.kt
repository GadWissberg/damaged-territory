package com.gadarts.returnfire.ecs.systems.player.handlers.movement.touchpad

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

class TurretTouchPadListener(
    private val movementHandler: VehicleMovementHandler,
    private val shootingHandler: PlayerShootingHandler,
    private val player: Entity,
) : ClickListener() {
    private var lastTouchDown: Long = 0

    override fun touchDown(
        event: InputEvent?,
        x: Float,
        y: Float,
        pointer: Int,
        button: Int
    ): Boolean {
        touchPadTouched(event!!.target)
        lastTouchDown = TimeUtils.millis()
        return super.touchDown(event, x, y, pointer, button)
    }

    override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        touchPadTouched(event!!.target)
        super.touchDragged(event, x, y, pointer)
    }

    override fun touchUp(
        event: InputEvent?,
        x: Float,
        y: Float,
        pointer: Int,
        button: Int
    ) {
        movementHandler.onTurretTouchPadTouchUp(player)
        shootingHandler.onTurretTouchPadTouchUp()
        super.touchUp(event, x, y, pointer, button)
    }

    private fun touchPadTouched(actor: Actor) {
        val deltaX = (actor as Touchpad).knobPercentX
        val deltaY = actor.knobPercentY
        movementHandler.onTurretTouchPadTouchDown(deltaX, deltaY, player)
        shootingHandler.onTurretTouchPadTouchDown(actor.knobPercentX, actor.knobPercentY)
    }
}
