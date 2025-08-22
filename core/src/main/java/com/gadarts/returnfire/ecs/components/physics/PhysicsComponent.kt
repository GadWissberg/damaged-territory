package com.gadarts.returnfire.ecs.components.physics

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool.Poolable

class PhysicsComponent : Component, Poolable, Disposable {
    var disposed: Boolean = false
        private set
    lateinit var rigidBody: RigidBody

    override fun reset() {

    }

    fun init(
        rigidBody: RigidBody,
    ) {
        this.rigidBody = rigidBody
        this.disposed = false
    }


    override fun dispose() {
        if (rigidBody.rigidBodyPool == null) {
            rigidBody.collisionShape.dispose()
            rigidBody.dispose()
            disposed = true
        }
    }

    companion object {
        val worldGravity = Vector3(0F, -10F, 0F)
    }
}
