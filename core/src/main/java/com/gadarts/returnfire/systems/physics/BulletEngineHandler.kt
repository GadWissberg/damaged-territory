package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.systems.data.GameSessionData

class BulletEngineHandler(
    private val globalData: GameSessionData,
    private val engine: Engine,
) : Disposable, EntityListener {

    private lateinit var debugDrawer: DebugDrawer
    private lateinit var broadPhase: btAxisSweep3
    private lateinit var ghostPairCallback: btGhostPairCallback
    private lateinit var solver: btSequentialImpulseConstraintSolver
    private lateinit var dispatcher: btCollisionDispatcher
    private lateinit var collisionConfiguration: btDefaultCollisionConfiguration


    private fun initializeDebug() {
        debugDrawer = DebugDrawer()
        debugDrawer.debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawWireframe
        globalData.gameSessionPhysicsData.collisionWorld.debugDrawer = debugDrawer
    }


    private fun initializeBroadPhase() {
        ghostPairCallback = btGhostPairCallback()
        val corner1 = Vector3(-100F, -100F, -100F)
        val corner2 = Vector3(100F, 100F, 100F)
        broadPhase = btAxisSweep3(corner1, corner2)
        broadPhase.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)
    }

    override fun entityAdded(entity: Entity?) {
    }

    fun addBodyOfEntity(entity: Entity) {
        if (ComponentsMapper.physics.has(entity)) {
            val btRigidBody: btRigidBody = ComponentsMapper.physics.get(entity).rigidBody
            if (ComponentsMapper.bullet.has(entity)) {
                globalData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody, 1, -1)
            } else if (ComponentsMapper.player.has(entity)) {
                globalData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody, 2, 4)
            } else {
                globalData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody)
            }
        }
    }

    override fun entityRemoved(entity: Entity?) {
        if (ComponentsMapper.physics.has(entity)) {
            val physicsComponent = ComponentsMapper.physics[entity]
            physicsComponent.rigidBody.activationState = 0
            globalData.gameSessionPhysicsData.collisionWorld.removeCollisionObject(physicsComponent.rigidBody)
            physicsComponent.dispose()
        }
    }


    private fun initializeCollisionWorld() {
        globalData.gameSessionPhysicsData.collisionWorld = btDiscreteDynamicsWorld(
            dispatcher,
            broadPhase,
            solver,
            collisionConfiguration
        )
    }

    override fun dispose() {
        collisionConfiguration.dispose()
        solver.dispose()
        dispatcher.dispose()
        ghostPairCallback.dispose()
        broadPhase.dispose()
        debugDrawer.dispose()
    }

    fun update(deltaTime: Float) {
        globalData.gameSessionPhysicsData.collisionWorld.stepSimulation(
            deltaTime,
            5,
            1f / Gdx.graphics.framesPerSecond
        )
    }

    fun initialize() {
        Bullet.init()
        collisionConfiguration = btDefaultCollisionConfiguration()
        dispatcher = btCollisionDispatcher(collisionConfiguration)
        solver = btSequentialImpulseConstraintSolver()
        initializeBroadPhase()
        initializeCollisionWorld()
        initializeDebug()
        globalData.gameSessionPhysicsData.debugDrawingMethod =
            object : CollisionShapesDebugDrawing {
                override fun drawCollisionShapes(camera: PerspectiveCamera) {
                    debugDrawer.begin(camera)
                    globalData.gameSessionPhysicsData.collisionWorld.debugDrawWorld()
                    debugDrawer.end()
                }
            }
        engine.addEntityListener(this)
    }


    companion object {
        val auxVector = Vector3()
    }

}
