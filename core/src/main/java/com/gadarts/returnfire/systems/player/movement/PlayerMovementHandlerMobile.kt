package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class PlayerMovementHandlerMobile : PlayerMovementHandler(0.5F) {
    private lateinit var camera: PerspectiveCamera
    private var rotToAdd = 0F
    private var desiredDirectionChanged: Boolean = false

    private fun handleRotation(deltaTime: Float, player: Entity) {
        if (desiredDirectionChanged && !thrustVelocity.isZero) {
            calculateRotation(deltaTime, player)
        } else {
            tiltAnimationHandler.lowerRotationTilt()
        }
        applyRotation(player)
    }

    private fun applyRotation(player: Entity) {
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val playerComponent = ComponentsMapper.player.get(player)
        val currentVelocity =
            playerComponent.getCurrentVelocity(auxVector2_1)
        transform.setToRotation(
            Vector3.Y,
            currentVelocity.angleDeg()
        )
        transform.rotate(Vector3.Z, -IDLE_Z_TILT_DEGREES)
        transform.setTranslation(position)
    }


    private fun calculateRotation(deltaTime: Float, player: Entity) {
        val rotBefore = rotToAdd
        updateRotationStep(player)
        val currentVelocity =
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1)
        val diff = abs(currentVelocity.angleDeg() - thrustVelocity.angleDeg())
        if ((rotBefore < 0 && rotToAdd < 0) || (rotBefore > 0 && rotToAdd > 0) && diff > ROT_EPSILON) {
            rotate(currentVelocity, deltaTime, player)
        } else {
            desiredDirectionChanged = false
        }
    }

    private fun rotate(currentVelocity: Vector2, deltaTime: Float, player: Entity) {
        currentVelocity.rotateDeg(rotToAdd * deltaTime)
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
        tiltAnimationHandler.onRotation(rotToAdd)
    }

    private fun updateRotationStep(player: Entity) {
        if (thrustVelocity.isZero) return
        val playerComponent = ComponentsMapper.player.get(player)
        val diff =
            thrustVelocity.angleDeg() - playerComponent.getCurrentVelocity(auxVector2_1)
                .angleDeg()
        val negativeRotation = auxVector2_1.set(1F, 0F).setAngleDeg(diff).angleDeg() > 180
        rotToAdd = if (negativeRotation && rotToAdd < 0) {
            max(rotToAdd - ROTATION_INCREASE, -MAX_ROTATION_STEP)
        } else if (!negativeRotation && rotToAdd > 0) {
            min(rotToAdd + ROTATION_INCREASE, MAX_ROTATION_STEP)
        } else {
            INITIAL_ROTATION_STEP * (if (negativeRotation) -1F else 1F)
        }
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        if (directionX != 0F || directionY != 0F) {
            updateDesiredDirection(directionX, directionY, player)
        } else {
            thrustVelocity.setZero()
        }
    }

    private fun updateDesiredDirection(deltaX: Float, deltaY: Float, player: Entity) {
        thrustVelocity.set(deltaX, deltaY)
        desiredDirectionChanged = true
        updateRotationStep(player)
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        handleRotation(deltaTime, player)
        handleAcceleration(player, MAX_THRUST, thrustVelocity)
        takeStep(
            player,
            deltaTime,
            currentMap,
            dispatcher,
            ComponentsMapper.player.get(player).getCurrentVelocity(
                auxVector2_1
            )
        )
        tiltAnimationHandler.update(player)
    }

    override fun rotate(player: Entity, clockwise: Int) {

    }

    override fun handleAcceleration(player: Entity, maxSpeed: Float, desiredVelocity: Vector2) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        if (thrustVelocity.len2() > desiredVelocitySizeThreshold) {
            currentVelocity.setLength2(
                min(currentVelocity.len2() + (ACCELERATION), MAX_THRUST)
            )
            tiltAnimationHandler.animateForwardAcceleration()
        } else {
            currentVelocity.setLength2(
                max(
                    currentVelocity.len2() - (DECELERATION),
                    1F
                )
            )
            tiltAnimationHandler.onDeceleration()
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    override fun onTouchUp(keycode: Int) {
        thrustVelocity.setZero()
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    companion object {
        private const val INITIAL_ROTATION_STEP = 6F
        private val auxVector2_1 = Vector2()
        private val auxVector3_1 = Vector3()
        private const val ROT_EPSILON = 0.5F
    }
}
