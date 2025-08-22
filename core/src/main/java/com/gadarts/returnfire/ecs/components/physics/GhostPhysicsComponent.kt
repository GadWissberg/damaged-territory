package com.gadarts.returnfire.ecs.components.physics

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable

class GhostPhysicsComponent(val ghost: btPairCachingGhostObject) : Component, Disposable {
    override fun dispose() {
        ghost.dispose()
    }


}
