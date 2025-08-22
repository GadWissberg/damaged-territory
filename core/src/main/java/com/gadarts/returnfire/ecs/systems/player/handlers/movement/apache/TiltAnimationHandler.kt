package com.gadarts.returnfire.ecs.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper

class TiltAnimationHandler {

    private var rollTarget: Float = ROLL_IDLE
    private var pitchTarget: Float = 0F

    fun update(character: Entity) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val rotation = modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(
            auxQuaternion
        )
        val roll = if (rotation.roll >= 0) rotation.roll else rotation.roll + 360F
        if (rollTarget != ROLL_IDLE || !MathUtils.isEqual(roll, ROLL_IDLE + 360F, 1.5F)) {
            if (roll < 45F || roll > (ROLL_REVERSE + 360F) || roll > (rollTarget + 360F)) {
                modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.Z, -ROTATION_STEP)
            } else if (roll > 45F && (roll < (ROLL_THRUST + 360F) || roll < (rollTarget + 360F))) {
                modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.Z, ROTATION_STEP)
            }
        }
        if (pitchTarget != 0F || !MathUtils.isEqual(rotation.pitch, 0F, 1.5F)) {
            if (rotation.pitch > pitchTarget + 360F || rotation.pitch > pitchTarget) {
                modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.X, -ROTATION_STEP)
            } else if (rotation.pitch < pitchTarget || rotation.pitch > 315F) {
                modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.X, ROTATION_STEP)
            }
        }
    }

    fun tiltForward() {
        rollTarget = ROLL_THRUST
    }

    fun returnToRollIdle() {
        rollTarget = ROLL_IDLE
    }

    fun tiltBackwards() {
        rollTarget = ROLL_REVERSE
    }

    fun lateralTilt(side: Int) {
        pitchTarget = (if (side == 0) 0F else (if (side > 0F) -1F else 1F)) * PITCH
    }

    fun returnToPitchIdle() {
        pitchTarget = 0F
    }

    companion object {
        private val auxQuaternion = Quaternion()
        private const val ROTATION_STEP = 1F
        private const val ROLL_IDLE = -12F
        private const val ROLL_THRUST = -24F
        private const val ROLL_REVERSE = -4F
        private const val PITCH = 10F
    }

}
