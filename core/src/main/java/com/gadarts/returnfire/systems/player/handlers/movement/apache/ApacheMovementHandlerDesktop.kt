package com.gadarts.returnfire.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.physics.RigidBody

class ApacheMovementHandlerDesktop : ApacheMovementHandler() {
    private var strafeActivated: Boolean = false
    private var movement: Int = 0
    private var strafe: Int = 0
    private var rotation: Int = 0

    override fun onMovementTouchUp(character: Entity, keycode: Int) {
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
                applyFunctionalityForHorizontalKeyRelease(rotation > 0F, character)
            }

            Input.Keys.RIGHT -> {
                applyFunctionalityForHorizontalKeyRelease(rotation < 0F, character)
            }

        }
    }

    private fun applyFunctionalityForHorizontalKeyRelease(rotationCheck: Boolean, character: Entity) {
        if (strafeActivated) {
            strafe = 0
            tiltAnimationHandler.returnToPitchIdle()
        } else if (rotationCheck) {
            applyRotation(0, character)
            tiltAnimationHandler.returnToPitchIdle()
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

    override fun pressedAlt(character: Entity) {
        strafeActivated = true
        if (rotation != 0) {
            strafe(rotation > 0)
            applyRotation(0, character)
        }
    }

    override fun pressedLeft(character: Entity) {
        applyHorizontalKeyFunctionality(true, character)
    }

    private fun applyHorizontalKeyFunctionality(left: Boolean, character: Entity) {
        if (strafeActivated) {
            strafe(left)
        } else {
            applyRotation(if (left) 1 else -1, character)
        }
    }

    override fun pressedRight(character: Entity) {
        applyHorizontalKeyFunctionality(false, character)
    }

    override fun releasedAlt(character: Entity) {
        strafeActivated = false
        if (isStrafing()) {
            applyRotation(strafe * -1, character)
            strafe = 0
        }
    }

    override fun isThrusting(character: Entity): Boolean {
        return movement > 0
    }

    override fun isReversing(character: Entity): Boolean {
        return movement < 0
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float, character: Entity) {

    }

    override fun onTurretTouchPadTouchUp(character: Entity) {

    }

    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        super.thrust(character, directionX, directionY)
        movement = MOVEMENT_FORWARD
    }

    override fun update(character: Entity, deltaTime: Float) {
        super.update(character, deltaTime)
        val physicsComponent = ComponentsMapper.physics.get(character) ?: return

        val rigidBody = physicsComponent.rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement, character)
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
        tiltAnimationHandler.lateralTilt(strafe * -1)
    }


    override fun applyRotation(clockwise: Int, character: Entity) {
        super.applyRotation(clockwise, character)
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
        private const val STRAFE_LEFT = -1
        private const val STRAFE_RIGHT = 1
        private val auxVector = Vector3()
    }
}
