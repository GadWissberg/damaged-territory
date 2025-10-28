package com.gadarts.returnfire.ecs.systems.data.session

import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.systems.data.CollisionShapesDebugDrawing

class GameSessionDataPhysics : Disposable {
    var debugDrawingMethod: CollisionShapesDebugDrawing? = null
    lateinit var collisionWorld: btDiscreteDynamicsWorld
    override fun dispose() {
        collisionWorld.dispose()
    }

}
