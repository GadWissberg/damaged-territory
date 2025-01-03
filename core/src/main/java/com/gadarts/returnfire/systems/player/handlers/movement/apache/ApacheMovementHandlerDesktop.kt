package com.gadarts.returnfire.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.physics.RigidBody

class ApacheMovementHandlerDesktop : ApacheMovementHandler() {
    private val strafeActivated: Boolean = false
    private var movement: Int = 0
    private var strafe: Int = 0
    private var rotation: Int = 0

    override fun onMovementTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    stopMovement()
                }
            }

            Input.Keys.DOWN -> {
                if (movement == MOVEMENT_REVERSE) {
                    stopMovement()
                }
            }

            Input.Keys.LEFT -> {
                if (rotation > 0F) {
                    rotation = 0
                    tiltAnimationHandler.returnToPitchIdle()
                }
            }

            Input.Keys.RIGHT -> {
                if (rotation < 0F) {
                    rotation = 0
                    tiltAnimationHandler.returnToPitchIdle()
                }
            }

        }
    }

    override fun stopMovement() {
        movement = 0
        tiltAnimationHandler.returnToRollIdle()
    }

    override fun strafe(left: Boolean) {
        strafe = if (left) STRAFE_LEFT else STRAFE_RIGHT
    }

    override fun isStrafing(): Boolean {
        return strafe != 0
    }

    override fun stopStrafe() {
        tiltAnimationHandler.returnToPitchIdle()
        strafe = 0
    }

    override fun pressedAlt() {
        TODO("Not yet implemented")
    }

    override fun pressedLeft() {
        if (strafeActivated) {

        }
    }

    override fun pressedRight() {
        TODO("Not yet implemented")
    }

    override fun releasedAlt() {
        TODO("Not yet implemented")
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {

    }

    override fun onTurretTouchPadTouchUp() {

    }

    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        super.thrust(character, directionX, directionY)
        movement = MOVEMENT_FORWARD
    }

    override fun update(player: Entity, deltaTime: Float) {
        super.update(player, deltaTime)
        val physicsComponent = ComponentsMapper.physics.get(player)
        val rigidBody = physicsComponent.rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement)
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
        if (strafe != 0) {
            pushSideWay(rigidBody)
        }
    }

    private fun pushSideWay(rigidBody: RigidBody) {
        val direction = auxVector.set(0F, 0F, strafe * 1F)
        push(rigidBody, direction, 25F)
        tiltAnimationHandler.lateralTilt(strafe)
    }


    override fun applyRotation(clockwise: Int) {
        super.applyRotation(clockwise)
        rotation = clockwise
    }

    override fun reverse() {
        movement = MOVEMENT_REVERSE
        tiltAnimationHandler.tiltBackwards()
    }

    companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
        private const val STRAFE_LEFT = 1
        private const val STRAFE_RIGHT = -1
        private val auxVector = Vector3()
    }
}
