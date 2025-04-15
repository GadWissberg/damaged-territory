package com.gadarts.returnfire.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

abstract class ApacheMovementHandler : VehicleMovementHandler(
    -15F,
    9F,
    50F,
    25F,
    7F
) {

    protected var tiltAnimationHandler = TiltAnimationHandler()

    override fun thrust(
        character: Entity,
        directionX: Float,
        directionY: Float,
    ) {
        tiltAnimationHandler.tiltForward()
    }

    private fun syncModelInstanceTransformToRigidBody(character: Entity) {
        if (!ComponentsMapper.physics.has(character)) return

        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(auxQuaternion1)
        auxQuaternion1.setEulerAngles(0F, auxQuaternion1.pitch, auxQuaternion1.roll)
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        modelInstanceComponent.gameModelInstance.modelInstance.transform.setToTranslation(
            rigidBody.worldTransform.getTranslation(
                auxVector3_1
            ),
        )
        auxQuaternion2.idt().setEulerAngles(
            rigidBody.worldTransform.getRotation(auxQuaternion3).yaw,
            auxQuaternion1.pitch,
            auxQuaternion1.roll
        )
        modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(auxQuaternion2)
    }

    override fun update(
        character: Entity,
        deltaTime: Float,
    ) {
        super.update(character, deltaTime)
        syncModelInstanceTransformToRigidBody(character)
        tiltAnimationHandler.update(character)
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
        if (clockwise == 0) return
        tiltAnimationHandler.lateralTilt(clockwise)
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxQuaternion1 = Quaternion()
        private val auxQuaternion2 = Quaternion()
        private val auxQuaternion3 = Quaternion()
    }
}
