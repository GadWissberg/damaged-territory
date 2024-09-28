package com.gadarts.returnfire.systems.player.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.gadarts.returnfire.components.ComponentsMapper

class ApacheMovementHandlerDesktop : ApacheMovementHandler() {
    private var movement: Int = 0
    private var rotation: Int = 0
    private lateinit var camera: PerspectiveCamera

    override fun onTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    movement = 0
                    tiltAnimationHandler.returnToRollIdle()
                }
            }

            Input.Keys.DOWN -> {
                if (movement == MOVEMENT_REVERSE) {
                    movement = 0
                    tiltAnimationHandler.returnToRollIdle()
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


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float) {
        super.thrust(player, directionX, directionY)
        movement = MOVEMENT_FORWARD
    }

    override fun update(player: Entity) {
        super.update(player)
        val physicsComponent = ComponentsMapper.physics.get(player)
        val rigidBody = physicsComponent.rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement)
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
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
    }
}
