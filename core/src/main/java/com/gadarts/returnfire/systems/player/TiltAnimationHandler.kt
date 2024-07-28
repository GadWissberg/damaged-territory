package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class TiltAnimationHandler {

    private var rotationTarget: Float = ANGLE_IDLE

    fun update(player: Entity) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(player)
        val rotation = modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(
            auxQuaternion
        )
        val roll = if (rotation.roll >= 0) rotation.roll else rotation.roll + 360F
        if (roll < 45F || roll > (ANGLE_REVERSE + 360F) || roll > (rotationTarget + 360F)) {
            modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.Z, -ROTATION_STEP)
        } else if (roll > 45F && (roll < (ANGLE_THRUST + 360F) || roll < (rotationTarget + 360F))) {
            modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(Vector3.Z, ROTATION_STEP)
        }
    }

    fun animateForwardAcceleration() {
        rotationTarget = ANGLE_THRUST
    }

    fun animateDeceleration() {
        rotationTarget = ANGLE_IDLE
    }

    companion object {
        private val auxQuaternion = Quaternion()
        private const val ROTATION_STEP = 1F
        private const val ANGLE_IDLE = -8F
        private const val ANGLE_THRUST = -20F
        private const val ANGLE_REVERSE = -2F
    }

}
