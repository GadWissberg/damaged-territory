package com.gadarts.returnfire.systems.data.pools

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.components.physics.MotionState
import com.gadarts.returnfire.components.physics.RigidBody

class RigidBodyFactory {
    fun create(
        mass: Float,
        shape: btCollisionShape,
        collisionFlag: Int?,
        rigidBodyPool: RigidBodyPool?,
        transform: Matrix4 = Matrix4()
    ): RigidBody {
        val localInertia = Vector3()
        if (mass == 0f) {
            localInertia.setZero()
        } else {
            shape.calculateLocalInertia(mass, localInertia)
        }
        val motionState = MotionState()
        motionState.transformObject = transform
        motionState.setWorldTransform(transform)
        val rigidBody = RigidBody(mass, motionState, shape, localInertia, rigidBodyPool)
        rigidBody.setSleepingThresholds(1f, 1f)
        rigidBody.deactivationTime = 5f
        rigidBody.activate()
        rigidBody.activationState = Collision.DISABLE_DEACTIVATION
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        rigidBody.friction = 1.5F
        if (collisionFlag != null) {
            rigidBody.collisionFlags = collisionFlag
        }
        return rigidBody
    }

}
