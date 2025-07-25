package com.gadarts.returnfire.systems.player.handlers.movement

import com.badlogic.ashley.core.Entity

class JeepMovementHandler : VehicleMovementHandler(
    -30F,
    4F,
    45F,
    25F,
    6F,
) {
    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
    }

    override fun reverse() {
    }

    override fun onMovementTouchUp(character: Entity, keycode: Int) {
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float, character: Entity) {
    }

    override fun onTurretTouchPadTouchUp(character: Entity) {
    }

    override fun strafe(left: Boolean) {
    }

    override fun isStrafing(): Boolean {
        return false
    }

    override fun stopStrafe() {
    }

    override fun pressedAlt(character: Entity) {
    }

    override fun pressedLeft(character: Entity) {
    }

    override fun pressedRight(character: Entity) {
    }

    override fun releasedAlt(character: Entity) {
    }

    override fun isThrusting(character: Entity): Boolean {
        return false
    }

    override fun isReversing(character: Entity): Boolean {
        return false
    }

}
