package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior

class BulletLogic {
    fun update(bullet: Entity, deltaTime: Float): Boolean {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        if (bulletComponent.createdTime + 3000L > TimeUtils.millis()) {
            if (bulletComponent.behavior == BulletBehavior.CURVE) {
                val physicsComponent = ComponentsMapper.physics.get(bullet)
                val curveRotationStepSize = CURVE_ROTATION_STEP * deltaTime
                if (physicsComponent.rigidBody.worldTransform.getRotation(auxQuat).roll > -90F) {
                    val worldTransform = physicsComponent.rigidBody.worldTransform
                    physicsComponent.rigidBody.worldTransform =
                        auxMatrix.set(worldTransform).rotate(Vector3.Z, curveRotationStepSize)
                    val orientation =
                        physicsComponent.rigidBody.worldTransform.getRotation(auxQuat)
                    val localZ = auxVector1.set(0F, 0F, 1F)
                    orientation.transform(localZ)
                    physicsComponent.rigidBody.linearVelocity =
                        physicsComponent.rigidBody.linearVelocity.rotate(
                            localZ,
                            curveRotationStepSize
                        )
                }
            }
        } else {
            return true
        }
        return false
    }

    companion object {
        private const val CURVE_ROTATION_STEP = -90F
        private val auxQuat = Quaternion()
        private val auxMatrix = Matrix4()
        private val auxVector1 = Vector3()
    }
}
