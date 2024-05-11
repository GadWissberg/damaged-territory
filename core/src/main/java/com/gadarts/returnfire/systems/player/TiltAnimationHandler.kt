package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class TiltAnimationHandler {

    var tilt = false

    private fun handleMovementTilt(player: Entity) {
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        val aroundZ = transform.getRotation(auxQuat).getAngleAround(Vector3.Z)
        if (tilt) {
            if ((aroundZ > ACC_TILT_RELATIVE_MAX_DEGREES)) {
                transform.rotate(Vector3.Z, -ACC_TILT_STEP_SIZE)
            }
        } else {
            if (aroundZ < BALANCED_ANGLE) {
                transform.rotate(Vector3.Z, ACC_TILT_STEP_SIZE)
            }
        }
    }

    fun update(player: Entity) {
        handleMovementTilt(player)
    }

    fun init(player: Entity) {
        ComponentsMapper.modelInstance.get(player).modelInstance.transform.rotate(Vector3.Z, BALANCED_ANGLE)
    }

    companion object {
        private const val ACC_TILT_STEP_SIZE = 0.5F
        private const val ACC_TILT_RELATIVE_MAX_DEGREES = 360F - 14F
        private const val BALANCED_ANGLE = 360F - 6F
        private val auxQuat = Quaternion()

    }
}
