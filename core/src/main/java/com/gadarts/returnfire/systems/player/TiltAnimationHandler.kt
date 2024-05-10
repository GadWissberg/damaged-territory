package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import kotlin.math.max
import kotlin.math.min

class TiltAnimationHandler {

    private var accelerationTiltDegrees: Float = 0.0f
    private var rotTiltDegrees: Float = 0.0f
    fun onStrafeActivated() {
        rotTiltDegrees = 0F
    }

    private fun increaseRotationTilt(rotToAdd: Float) {
        rotTiltDegrees += if (rotToAdd > 0) -ROT_TILT_STEP_SIZE else ROT_TILT_STEP_SIZE
        rotTiltDegrees = MathUtils.clamp(rotTiltDegrees, -ROT_TILT_MAX_DEG, ROT_TILT_MAX_DEG)
    }

    fun lowerRotationTilt() {
        if (rotTiltDegrees > 0) {
            rotTiltDegrees = max(rotTiltDegrees - ROT_TILT_DEC_STEP_SIZE, 0F)
        } else if (rotTiltDegrees < 0) {
            rotTiltDegrees = min(rotTiltDegrees + ROT_TILT_DEC_STEP_SIZE, 0F)
        }
    }

    private fun handleMovementTilt(player: Entity) {
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        if (accelerationTiltDegrees > 0) {
            transform.rotate(Vector3.Z, -accelerationTiltDegrees)
        }
        val playerComponent = ComponentsMapper.player.get(player)
        if (playerComponent.strafing == null) {
            val degrees = if (playerComponent.strafing == null) rotTiltDegrees else -rotTiltDegrees
            transform.rotate(Vector3.X, degrees)
        }
    }

    fun update(player: Entity) {
        handleMovementTilt(player)
    }

    fun onAcceleration() {
        accelerationTiltDegrees = min(
            accelerationTiltDegrees + ACC_TILT_STEP_SIZE, ACC_TILT_RELATIVE_MAX_DEGREES
        )
    }

    fun onDeceleration() {
        accelerationTiltDegrees = max(accelerationTiltDegrees - ACC_TILT_STEP_SIZE, 0F)
    }

    fun onRotation(rotToAdd: Float) {
        increaseRotationTilt(rotToAdd)
    }

    companion object {
        private const val ROT_TILT_DEC_STEP_SIZE = 0.5F
        private const val ACC_TILT_STEP_SIZE = 0.5F
        private const val ACC_TILT_RELATIVE_MAX_DEGREES = 6F
        private const val ROT_TILT_MAX_DEG = 20F
        private const val ROT_TILT_STEP_SIZE = 1F

    }
}
