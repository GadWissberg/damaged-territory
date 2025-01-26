package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class TankMovementHandlerDesktop(player: Entity) :
    TankMovementHandler(player) {
    private var turretRotationEnabled: Boolean = false
    private var movement: Int = 0
    private var rotation: Int = 0

    override fun idleEngineSound() {
        super.idleEngineSound()
        movement = 0
    }

    override fun onMovementTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                stopFunctionForVerticalKey(MOVEMENT_FORWARD)
            }

            Input.Keys.DOWN -> {
                stopFunctionForVerticalKey(MOVEMENT_REVERSE)
            }

            Input.Keys.LEFT -> {
                stopFunctionForSideKey(rotation > 0F)
            }

            Input.Keys.RIGHT -> {
                stopFunctionForSideKey(rotation < 0F)
            }

        }
    }

    private fun stopFunctionForVerticalKey(movementConstant: Int) {
        if (movement == movementConstant) {
            idleEngineSound()
        }
    }

    private fun stopFunctionForSideKey(rotationCheck: Boolean) {
        if (turretRotationEnabled) {
            turretRotating = 0
        } else if (rotationCheck) {
            rotation = 0
            val rigidBody = ComponentsMapper.physics.get(player).rigidBody
            rigidBody.angularFactor = Vector3.Zero
        }
    }


    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        movement = MOVEMENT_FORWARD
    }

    override fun update(player: Entity, deltaTime: Float) {
        super.update(player, deltaTime)
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement)
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {

    }

    override fun onTurretTouchPadTouchUp() {

    }

    override fun strafe(left: Boolean) {
    }

    override fun isStrafing(): Boolean {
        return false
    }

    override fun stopStrafe() {
    }

    override fun pressedAlt() {
        turretRotationEnabled = true
        if (rotation != 0) {
            turretRotating = rotation
            applyRotation(0)
        }
    }

    override fun pressedLeft() {
        if (turretRotationEnabled) {
            turretRotating = 1
        } else {
            applyRotation(1)
        }
    }

    override fun pressedRight() {
        if (turretRotationEnabled) {
            turretRotating = -1
        } else {
            applyRotation(-1)
        }
    }

    override fun releasedAlt() {
        turretRotationEnabled = false
        if (turretRotating != 0) {
            applyRotation(turretRotating)
        }
        turretRotating = 0
    }

    override fun isThrusting(): Boolean {
        return movement > 0
    }

    override fun isReversing(): Boolean {
        return movement < 0
    }

    override fun applyRotation(clockwise: Int) {
        super.applyRotation(clockwise)
        rotation = clockwise
    }

    override fun reverse() {
        movement = MOVEMENT_REVERSE
    }

    companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
    }
}
