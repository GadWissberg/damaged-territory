package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.player.TiltAnimationHandler

abstract class PlayerMovementHandler {

    protected var tiltAnimationHandler = TiltAnimationHandler()


    protected fun rotate(rigidBody: btRigidBody, clockwise: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
        if (newVelocity.len2() < MAX_ROTATION) {
            val scale = clockwise * 8F
            rigidBody.applyTorque(auxVector3_1.set(0F, 1F, 0F).scl(scale))
            auxVector3_1.set(rigidBody.angularVelocity)
            auxVector3_1.x = 0F
            auxVector3_1.z = 0F
            rigidBody.angularVelocity = auxVector3_1
        }
    }

    private fun applyLateralDamping(player: Entity) {
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        val velocity: Vector3 = rigidBody.linearVelocity
        val lateralVelocity = auxVector3_1.set(velocity.x, 0f, velocity.z)
        val dampingForce: Vector3 = lateralVelocity.scl(-15F)
        rigidBody.applyCentralForce(dampingForce)
    }

    protected fun pushForward(rigidBody: btRigidBody, forwardDirection: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
        if (newVelocity.len2() < MAX_VELOCITY) {
            val forward =
                rigidBody.worldTransform.getRotation(auxQuaternion1)
                    .transform(auxVector3_2.set(forwardDirection * 1F, 0F, 0F))
            rigidBody.applyCentralForce(
                auxVector3_1.set(forward.x, 0F, forward.z)
                    .scl(if (forwardDirection > 0) 50F else 25F)
            )
        }
    }

    abstract fun onTouchUp(keycode: Int = -1)

    abstract fun initialize(camera: PerspectiveCamera)

    open fun thrust(
        player: Entity,
        directionX: Float = 0F,
        directionY: Float = 0F,
    ) {
        tiltAnimationHandler.tiltForward()
    }

    private fun syncModelInstanceTransformToRigidBody(player: Entity) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(player)
        modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(auxQuaternion1)
        auxQuaternion1.setEulerAngles(0F, auxQuaternion1.pitch, auxQuaternion1.roll)
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
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

    open fun update(
        player: Entity,
    ) {
        syncModelInstanceTransformToRigidBody(player)
        applyLateralDamping(player)
        tiltAnimationHandler.update(player)
    }

    open fun applyRotation(clockwise: Int) {
        tiltAnimationHandler.lateralTilt(clockwise)
    }

    abstract fun reverse(player: Entity)

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_ROTATION = 3F
        private val auxQuaternion1 = Quaternion()
        private val auxQuaternion2 = Quaternion()
        private val auxQuaternion3 = Quaternion()
        private const val MAX_VELOCITY = 6F
    }
}
