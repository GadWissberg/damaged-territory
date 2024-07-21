package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.systems.data.GameSessionData

class BulletEngineHandler(private val globalData: GameSessionData, engine: Engine) : Disposable, EntityListener {

    private lateinit var debugDrawer: DebugDrawer
    private lateinit var broadPhase: btAxisSweep3
    private lateinit var ghostPairCallback: btGhostPairCallback
    private var solver: btSequentialImpulseConstraintSolver
    private var dispatcher: btCollisionDispatcher
    private var collisionConfiguration: btDefaultCollisionConfiguration

    init {
        Bullet.init()
        collisionConfiguration = btDefaultCollisionConfiguration()
        dispatcher = btCollisionDispatcher(collisionConfiguration)
        solver = btSequentialImpulseConstraintSolver()
        initializeBroadPhase()
        initializeCollisionWorld()
        initializeDebug()
        val btRigidBody = createGroundPhysicsBody()
        globalData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody)
        globalData.gameSessionPhysicsData.debugDrawingMethod = object : CollisionShapesDebugDrawing {
            override fun drawCollisionShapes(camera: PerspectiveCamera) {
                debugDrawer.begin(camera)
                globalData.gameSessionPhysicsData.collisionWorld.debugDrawWorld()
                debugDrawer.end()
            }
        }
        btRigidBody.userData = EntityBuilder.begin().addGroundComponent().finishAndAddToEngine()
        engine.addEntityListener(this)
    }

    private fun initializeDebug() {
        debugDrawer = DebugDrawer()
        debugDrawer.debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawWireframe
        globalData.gameSessionPhysicsData.collisionWorld.debugDrawer = debugDrawer
    }

    private fun createGroundPhysicsBody(): btRigidBody {
        val ground = btStaticPlaneShape(auxVector.set(0F, 1F, 0F), 0F)
        val info = btRigidBody.btRigidBodyConstructionInfo(
            0f,
            null,
            ground
        )
        val btRigidBody = btRigidBody(info)
        info.dispose()
        btRigidBody.collisionFlags =
            btRigidBody.collisionFlags or btCollisionObject.CollisionFlags.CF_STATIC_OBJECT
        btRigidBody.contactCallbackFlag = btBroadphaseProxy.CollisionFilterGroups.KinematicFilter
        return btRigidBody
    }

    private fun initializeBroadPhase() {
        ghostPairCallback = btGhostPairCallback()
        val corner1 = Vector3(-100F, -100F, -100F)
        val corner2 = Vector3(100F, 100F, 100F)
        broadPhase = btAxisSweep3(corner1, corner2)
        broadPhase.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)
    }

    override fun entityAdded(entity: Entity?) {
        if (ComponentsMapper.physics.has(entity)) {
            val btRigidBody: btRigidBody = ComponentsMapper.physics.get(entity).rigidBody
            globalData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody)
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
        globalData.gameSessionPhysicsData.collisionWorld.gravity = Vector3(0F, GRAVITY_FORCE, 0F)
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
            1f / 60F
        )
    }


    companion object {
        const val GRAVITY_FORCE = -9.8f
        val auxVector = Vector3()
    }

}
