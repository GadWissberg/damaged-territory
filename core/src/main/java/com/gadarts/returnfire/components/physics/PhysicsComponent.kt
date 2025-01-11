package com.gadarts.returnfire.components.physics

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool.Poolable

class PhysicsComponent : Component, Poolable, Disposable {
    lateinit var rigidBody: RigidBody

    override fun reset() {

    }

    fun init(
        rigidBody: RigidBody,
    ) {
        this.rigidBody = rigidBody
    }


    override fun dispose() {
        if (rigidBody.rigidBodyPool == null) {
            rigidBody.collisionShape.dispose()
            rigidBody.dispose()
        }
    }

}
