package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap


class PlayerMovementHandlerDesktop(private val cachedBoundingBox: BoundingBox) : PlayerMovementHandler() {
    private var movement: Int = 0
    private var rotation: Int = 0
    private lateinit var camera: PerspectiveCamera

    init {

    }

    override fun handleAcceleration(player: Entity, maxSpeed: Float, desiredVelocity: Vector2) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        if (!desiredVelocity.isZero) {
            currentVelocity.add(desiredVelocity)
            if (currentVelocity.len2() > maxSpeed) {
                currentVelocity.setLength2(maxSpeed)
            }
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    override fun onTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    movement = MOVEMENT_IDLE
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
                rigidBody.applyCentralForce(forward.scl(if (movement > MOVEMENT_IDLE) 50F else 25F))
            }
        }
        if (rotation != ROTATION_IDLE) {
            val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
            if (newVelocity.len2() < MAX_ROTATION) {
                rigidBody.applyTorque(auxVector3_1.set(0F, cachedBoundingBox.centerX + 1F, 0F).scl(rotation * 8F))
            }
        }
    }


    override fun rotate(clockwise: Int) {
        rotation = clockwise
    }

    override fun reverse(player: Entity) {
        movement = MOVEMENT_REVERSE
    }

    companion object {
        private val auxVector2_1 = Vector2()
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
