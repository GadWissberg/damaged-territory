package com.gadarts.returnfire.components.physics

import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.GameComponent

class PhysicsComponent : GameComponent(), Disposable {
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
            rigidBody.dispose()
        }
    }

}
