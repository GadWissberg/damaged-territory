package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap


class PlayerMovementHandlerDesktop(private val cachedBoundingBox: BoundingBox) : PlayerMovementHandler() {
    private var movement: Int = 0
    private var rotation: Int = 0
    private lateinit var camera: PerspectiveCamera

    override fun onTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    movement = MOVEMENT_IDLE
                    tiltAnimationHandler.animateDeceleration()
                }
            }

            Input.Keys.DOWN -> {
                if (movement == MOVEMENT_REVERSE) {
                    movement = MOVEMENT_IDLE
                }
            }

            Input.Keys.LEFT -> {
                if (rotation > 0F) {
                    rotation = 0
                }
            }

            Input.Keys.RIGHT -> {
                if (rotation < 0F) {
                    rotation = 0
                }
            }

        }
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        movement = MOVEMENT_FORWARD
        tiltAnimationHandler.animateForwardAcceleration()
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        if (movement != MOVEMENT_IDLE) {
            val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
            if (newVelocity.len2() < MAX_VELOCITY) {
                val forward =
                    rigidBody.worldTransform.getRotation(auxQuaternion)
                        .transform(auxVector3_2.set(movement * 1F, 0F, 0F))
                rigidBody.applyCentralForce(
                    auxVector3_1.set(forward.x, 0F, forward.z).scl(if (movement > MOVEMENT_IDLE) 50F else 25F)
                )
            }
        }
        if (rotation != ROTATION_IDLE) {
            val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
            if (newVelocity.len2() < MAX_ROTATION) {
                val scale = rotation * 8F
                rigidBody.applyTorque(auxVector3_1.set(0F, 1F, 0F).scl(scale))
                auxVector3_1.set(rigidBody.angularVelocity)
                auxVector3_1.x = 0F
                auxVector3_1.z = 0F

                rigidBody.angularVelocity = auxVector3_1
            }
        }
        tiltAnimationHandler.update(player)
    }


    override fun rotate(clockwise: Int) {
        rotation = clockwise
    }

    override fun reverse(player: Entity) {
        movement = MOVEMENT_REVERSE
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_VELOCITY = 6F
        private const val MAX_ROTATION = 3F
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_IDLE = 0
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
        private val auxQuaternion = Quaternion()
    }
}
