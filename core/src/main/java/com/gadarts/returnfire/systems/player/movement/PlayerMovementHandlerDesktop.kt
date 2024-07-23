package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap


class PlayerMovementHandlerDesktop : PlayerMovementHandler(0F) {
    private var thrusting: Boolean = false
    private var rotating: Int = 0
    private lateinit var camera: PerspectiveCamera
    private val reverseVelocity: Vector2 = Vector2(desiredVelocitySizeThreshold, 0F)

    override fun handleAcceleration(player: Entity, maxSpeed: Float, desiredVelocity: Vector2) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        if (!desiredVelocity.isZero) {
            currentVelocity.add(desiredVelocity)
            if (currentVelocity.len2() > maxSpeed) {
                currentVelocity.setLength2(maxSpeed)
            }
            if (desiredVelocity == thrustVelocity) {
                tiltAnimationHandler.animateForwardAcceleration()
            }
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    override fun onTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                thrusting = false
            }

            Input.Keys.DOWN -> {
                reverseVelocity.setZero()
            }

            Input.Keys.LEFT -> {
                if (rotating > 0F) {
                    rotating = 0
                }
            }

            Input.Keys.RIGHT -> {
                if (rotating < 0F) {
                    rotating = 0
                }
            }

        }
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        thrusting = true
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        if (thrusting) {
            val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
            if (newVelocity.len2() < MAX_VELOCITY) {
                rigidBody.applyCentralForce(auxVector3_1.set(1F, 0F, 0F).scl(25F))
            }
        } else {
            rigidBody.applyCentralForce(auxVector3_1.set(rigidBody.linearVelocity).scl(-rigidBody.linearDamping))
        }
    }


    override fun rotate(clockwise: Int) {
        rotating = clockwise * 3
    }

    companion object {
        private val auxVector2_1 = Vector2()
        private val auxVector3_1 = Vector3()
        private const val MAX_VELOCITY = 3F
    }
}
