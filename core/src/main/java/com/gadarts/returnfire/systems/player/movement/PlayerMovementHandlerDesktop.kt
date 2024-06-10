package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap
import kotlin.math.max
import kotlin.math.min


class PlayerMovementHandlerDesktop : PlayerMovementHandler(0F) {
    private var rotating: Int = 0
    private var rotation = 0F
    private var direction = 0F
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

    override fun onTouchUp(player: Entity, keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                thrustVelocity.setZero()
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
        if (!reverse) {
            thrustVelocity.set(ACCELERATION, 0F)
        } else {
            reverseVelocity.set(-0.02F, 0F)
        }
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        applyRotation(player, deltaTime)
        handleAcceleration(player, MAX_THRUST, thrustVelocity)
        handleAcceleration(player, 4F, reverseVelocity)
        if (thrustVelocity.isZero && reverseVelocity.isZero) {
            val currentVelocity =
                ComponentsMapper.player.get(player)
                    .getCurrentVelocity(auxVector2_1)
            if (currentVelocity.x > 0F) {
                currentVelocity.set(max(currentVelocity.x - DECELERATION, 0F), 0F)
            } else if (currentVelocity.x < 0F) {
                currentVelocity.set(min(currentVelocity.x + DECELERATION, 0F), 0F)
            }
            tiltAnimationHandler.onDeceleration()
            ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
        }
        val currentVelocity = ComponentsMapper.player.get(player).getCurrentVelocity(
            auxVector2_2
        )
        takeStep(
            player,
            deltaTime,
            currentMap,
            dispatcher,
            auxVector2_1.set(1F, 0F)
                .setAngleDeg(direction + (if (currentVelocity.x > 0F) 0F else +180F)).setLength2(
                    currentVelocity.len2()
                )
        )
        tiltAnimationHandler.update(player)
    }

    private fun applyRotation(player: Entity, deltaTime: Float) {
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        if (rotating != 0) {
            rotation += rotating
            rotation =
                MathUtils.clamp(rotation, -MAX_ROTATION_STEP, MAX_ROTATION_STEP)
            tiltAnimationHandler.onRotation(rotation)
        } else if (rotation > 0) {
            rotation = max(rotation - ROTATION_DECELERATION, 0F)
            tiltAnimationHandler.lowerRotationTilt()
        } else if (rotation < 0) {
            rotation = min(rotation + ROTATION_DECELERATION, 0F)
            tiltAnimationHandler.lowerRotationTilt()
        }
        direction += rotation * deltaTime
        transform.setToRotation(Vector3.Y, direction)
        transform.rotate(Vector3.Z, -IDLE_Z_TILT_DEGREES)
        transform.setTranslation(position)
    }

    override fun rotate(player: Entity, clockwise: Int) {
        rotating = clockwise * 3
    }

    companion object {
        private val auxVector2_1 = Vector2()
        private val auxVector2_2 = Vector2()
        private val auxVector3_1 = Vector3()
        private const val ROTATION_DECELERATION = 3F
        private const val MAX_ROTATION_STEP = 100F
    }
}
