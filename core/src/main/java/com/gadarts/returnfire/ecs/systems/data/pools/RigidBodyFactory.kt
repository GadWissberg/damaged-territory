package com.gadarts.returnfire.ecs.systems.data.pools

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.components.physics.MotionState
import com.gadarts.returnfire.components.physics.RigidBody

class RigidBodyFactory {
    fun create(
        mass: Float,
        shape: btCollisionShape,
        rigidBodyPool: RigidBodyPool?,
        transform: Matrix4 = Matrix4(),
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
        return rigidBody
    }

}
