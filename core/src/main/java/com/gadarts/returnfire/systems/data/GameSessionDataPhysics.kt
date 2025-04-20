package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.utils.Disposable

class GameSessionDataPhysics : Disposable {
    lateinit var ghostObject: btPairCachingGhostObject
    var debugDrawingMethod: CollisionShapesDebugDrawing? = null
    lateinit var collisionWorld: btDiscreteDynamicsWorld
    override fun dispose() {
        collisionWorld.dispose()
        ghostObject.dispose()
    }

}
