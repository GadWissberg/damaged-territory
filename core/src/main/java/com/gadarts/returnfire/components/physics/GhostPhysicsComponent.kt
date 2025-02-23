package com.gadarts.returnfire.components.physics

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils

class GhostPhysicsComponent(val ghost: btPairCachingGhostObject, ttl: Long) : Component, Disposable {
    val destructionTime: Long = TimeUtils.millis() + ttl
    override fun dispose() {
        ghost.dispose()
    }


}
