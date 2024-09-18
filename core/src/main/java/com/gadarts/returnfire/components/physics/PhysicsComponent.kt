package com.gadarts.returnfire.components.physics

import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.GameComponent

class PhysicsComponent : GameComponent(), Disposable {
    lateinit var rigidBody: btRigidBody

    override fun reset() {

    }

    fun init(
        rigidBody: btRigidBody,
    ) {
        this.rigidBody = rigidBody
    }


    override fun dispose() {
        rigidBody.dispose()
    }

}
