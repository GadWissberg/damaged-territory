package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class TiltAnimationHandler {

    var tiltForward = 0F

    private fun handleForwardMovementTilt(player: Entity) {
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        val aroundAxis = transform.getRotation(auxQuat).getAngleAround(Vector3.Z)
        if (tiltForward > 0F) {
            if ((aroundAxis > 360F - FORWARD_ACC_TILT_RELATIVE_MAX_DEGREES || aroundAxis < 180F)) {
                transform.rotate(Vector3.Z, -ACC_TILT_STEP_SIZE)
            }
        } else if (tiltForward < 0F) {
            if ((aroundAxis < FORWARD_ACC_TILT_RELATIVE_MAX_DEGREES || aroundAxis > 180F)) {
                transform.rotate(Vector3.Z, ACC_TILT_STEP_SIZE)
            }
        } else {
            if (aroundAxis < FORWARD_BALANCED_ANGLE && aroundAxis > 180) {
                transform.rotate(Vector3.Z, ACC_TILT_STEP_SIZE)
            } else if ((aroundAxis >= 0F && aroundAxis < 180F) || aroundAxis > FORWARD_BALANCED_ANGLE) {
                transform.rotate(Vector3.Z, -ACC_TILT_STEP_SIZE)
            }
        }
    }

    fun update(player: Entity) {
        handleForwardMovementTilt(player)
    }

    fun init(player: Entity) {
        ComponentsMapper.modelInstance.get(player).modelInstance.transform.rotate(Vector3.Z, FORWARD_BALANCED_ANGLE)
    }

    companion object {
        private const val ACC_TILT_STEP_SIZE = 0.5F
        private const val FORWARD_ACC_TILT_RELATIVE_MAX_DEGREES = 14F
        private const val FORWARD_BALANCED_ANGLE = 360F - 6F
        private val auxQuat = Quaternion()

    }
}
