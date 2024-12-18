package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.physics.RigidBody

class TankMovementHandlerDesktop(private val rigidBody: RigidBody, player: Entity) :
    TankMovementHandler(rigidBody, player) {
    private var movement: Int = 0
    private var rotation: Int = 0

    override fun idleEngineSound() {
        super.idleEngineSound()
        movement = 0
    }

    override fun onMovementTouchPadTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    idleEngineSound()
                }
            }

            Input.Keys.DOWN -> {
                if (movement == MOVEMENT_REVERSE) {
                    idleEngineSound()
                }
            }

            Input.Keys.LEFT -> {
                if (rotation > 0F) {
                    rotation = 0
                    rigidBody.angularFactor = Vector3.Zero
                }
            }

            Input.Keys.RIGHT -> {
                if (rotation < 0F) {
                    rotation = 0
                    rigidBody.angularFactor = Vector3.Zero
                }
            }

        }
    }


    override fun thrust(player: Entity, directionX: Float, directionY: Float) {
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

    override fun letterPressedD() {
        turretRotating = -1
    }

    override fun letterReleasedD() {
        turretRotating = 0
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {

    }

    override fun onTurretTouchPadTouchUp() {

    }

    override fun letterPressedA() {
        turretRotating = 1
    }

    override fun letterReleasedA() {
        turretRotating = 0
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
