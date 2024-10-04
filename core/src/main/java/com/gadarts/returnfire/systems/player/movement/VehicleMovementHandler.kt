package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.TurretCharacterDefinition


abstract class VehicleMovementHandler(
    private val lateralVelocityScale: Float,
    private val rotationScale: Float,
    private val forwardForceSize: Float,
    private val reverseForceSize: Float
) {
    abstract fun initialize(camera: PerspectiveCamera)

    protected fun pushForward(rigidBody: btRigidBody, forwardDirection: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
        if (newVelocity.len2() < MAX_VELOCITY) {
            val forward =
                rigidBody.worldTransform.getRotation(auxQuaternion1)
                    .transform(auxVector3_2.set(forwardDirection * 1F, 0F, 0F))
            rigidBody.applyCentralForce(
                auxVector3_1.set(forward.x, 0F, forward.z)
                    .scl(if (forwardDirection > 0) forwardForceSize else reverseForceSize)
            )
        }
    }

    open fun update(
        player: Entity,
    ) {
        applyLateralDamping(player)
        if (ComponentsMapper.character.get(player).definition == TurretCharacterDefinition.TANK) {
            val rigidBody = ComponentsMapper.physics.get(player).rigidBody
            if (!rigidBody.linearVelocity.epsilonEquals(Vector3.Zero)) {
                val transform: Matrix4 = rigidBody.worldTransform
                transform.getRotation(auxQuaternion1)
                val localZ = auxVector3_1.set(Vector3.Z)
                auxQuaternion1.transform(localZ)
                val velocity: Vector3 = rigidBody.linearVelocity
                val sidewaysVelocity = velocity.dot(localZ)
                velocity.mulAdd(localZ, -sidewaysVelocity)
                rigidBody.linearVelocity = velocity
            }
        }
    }

    abstract fun thrust(
        player: Entity,
        directionX: Float = 0F,
        directionY: Float = 0F,
    )

    abstract fun reverse()

    abstract fun onTouchUp(keycode: Int = -1)

    protected fun rotate(rigidBody: btRigidBody, clockwise: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
        if (newVelocity.len2() < MAX_ROTATION) {
            val scale = clockwise * rotationScale
            rigidBody.applyTorque(auxVector3_1.set(0F, 1F, 0F).scl(scale))
            auxVector3_1.set(rigidBody.angularVelocity)
            auxVector3_1.x = 0F
            auxVector3_1.z = 0F
            rigidBody.angularVelocity = auxVector3_1
        }
    }

    abstract fun applyRotation(clockwise: Int)

    private fun applyLateralDamping(player: Entity) {
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        val velocity: Vector3 = rigidBody.linearVelocity
        val lateralVelocity = auxVector3_1.set(velocity.x, 0f, velocity.z)
        val dampingForce: Vector3 = lateralVelocity.scl(lateralVelocityScale)
        rigidBody.applyCentralForce(dampingForce)
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_ROTATION = 3F
        private const val MAX_VELOCITY = 6F
        private val auxQuaternion1 = Quaternion()

    }
}
