package com.gadarts.returnfire.ecs.systems.physics

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy.CollisionFilterGroups.AllFilter
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.ecs.systems.data.GameSessionData

class BulletEngineHandler(
    private val gameSessionData: GameSessionData,
    private val engine: Engine,
) : Disposable, EntityListener {

    private val debugDrawer: DebugDrawer by lazy { DebugDrawer() }
    private val broadPhase: btAxisSweep3 by lazy {
        val corner1 = Vector3(-100F, -100F, -100F)
        val corner2 = Vector3(100F, 100F, 100F)
        btAxisSweep3(corner1, corner2)
    }
    private val ghostPairCallback: btGhostPairCallback by lazy { btGhostPairCallback() }
    private val solver: btSequentialImpulseConstraintSolver by lazy { btSequentialImpulseConstraintSolver() }
    private val dispatcher: btCollisionDispatcher by lazy {
        btCollisionDispatcher(
            collisionConfiguration
        )
    }
    private val collisionConfiguration: btDefaultCollisionConfiguration by lazy { btDefaultCollisionConfiguration() }

    private fun initializeDebug() {
        debugDrawer.debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawWireframe
        gameSessionData.physicsData.collisionWorld.debugDrawer = debugDrawer
    }


    private fun initializeBroadPhase() {
        broadPhase.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)
    }

    override fun entityAdded(entity: Entity) {
    }

    fun addBodyOfEntityToCollisionWorld(entity: Entity) {
        val collisionWorld = gameSessionData.physicsData.collisionWorld
        if (ComponentsMapper.physics.has(entity)) {
            val isBullet = ComponentsMapper.bullet.has(entity)
            val btRigidBody: btRigidBody = ComponentsMapper.physics.get(entity).rigidBody
            if (isBullet) {
                val bulletComponent = ComponentsMapper.bullet.get(entity)
                val friendly = bulletComponent.friendly
                val mask =
                    0x11111111 xor (if (friendly) COLLISION_GROUP_PLAYER_BULLET else COLLISION_GROUP_AI_BULLET)
                btRigidBody.contactCallbackFilter = mask
                collisionWorld.addRigidBody(
                    btRigidBody,
                    if (friendly) COLLISION_GROUP_PLAYER_BULLET else COLLISION_GROUP_AI_BULLET,
                    mask
                )
            } else if (ComponentsMapper.player.has(entity)) {
                val mask =
                    COLLISION_GROUP_AI_BULLET or COLLISION_GROUP_AI or COLLISION_GROUP_GROUND or COLLISION_GROUP_GENERAL
                btRigidBody.contactCallbackFilter = mask
                collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_PLAYER,
                    mask
                )
            } else if (ComponentsMapper.baseAi.has(entity)) {
                collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_AI,
                    COLLISION_GROUP_PLAYER or COLLISION_GROUP_AI or COLLISION_GROUP_GENERAL or COLLISION_GROUP_PLAYER_BULLET or COLLISION_GROUP_GROUND
                )
            } else if (ComponentsMapper.ground.has(entity)) {
                btRigidBody.contactCallbackFilter = AllFilter
                collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_GROUND,
                    AllFilter
                )
            } else if (ComponentsMapper.flyingPart.has(entity)) {
                collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_FLYING_PART,
                    COLLISION_GROUP_GROUND
                )
            } else {
                btRigidBody.contactCallbackFilter = AllFilter
                collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_GENERAL,
                    AllFilter
                )
            }
        } else if (ComponentsMapper.ghostPhysics.has(entity)) {
            val ghostObject = ComponentsMapper.ghostPhysics.get(entity).ghost
            collisionWorld.addCollisionObject(
                ghostObject,
                COLLISION_GROUP_GENERAL,
                COLLISION_GROUP_AI_BULLET or COLLISION_GROUP_AI
            )
        }
    }

    override fun entityRemoved(entity: Entity) {
        val physicsComponent = ComponentsMapper.physics.get(entity)
        if (physicsComponent != null) {
            removePhysicsOfComponent(entity)
        } else if (ComponentsMapper.ghostPhysics.has(entity)) {
            val ghostPhysicsComponent = ComponentsMapper.ghostPhysics.get(entity)
            gameSessionData.physicsData.collisionWorld.removeCollisionObject(ghostPhysicsComponent.ghost)
            ghostPhysicsComponent.dispose()
        }
    }

    fun removePhysicsOfComponent(entity: Entity) {
        val physicsComponent = ComponentsMapper.physics.get(entity)
        if (physicsComponent.disposed || physicsComponent.rigidBody.isDisposed) return

        gameSessionData.physicsData.collisionWorld.removeCollisionObject(
            physicsComponent.rigidBody
        )
        physicsComponent.dispose()
    }


    private fun initializeCollisionWorld() {
        gameSessionData.physicsData.collisionWorld = btDiscreteDynamicsWorld(
            dispatcher,
            broadPhase,
            solver,
            collisionConfiguration
        )
        gameSessionData.physicsData.collisionWorld.gravity = Vector3(0F, -10F, 0F)
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
        gameSessionData.physicsData.collisionWorld.stepSimulation(
            deltaTime,
            4,
            1f / gameSessionData.fpsTarget
        )
    }

    fun initialize() {
        initializeBroadPhase()
        initializeCollisionWorld()
        initializeDebug()
        gameSessionData.physicsData.debugDrawingMethod =
            object : CollisionShapesDebugDrawing {
                override fun drawCollisionShapes(camera: PerspectiveCamera) {
                    debugDrawer.begin(camera)
                    gameSessionData.physicsData.collisionWorld.debugDrawWorld()
                    debugDrawer.end()
                }
            }
        engine.addEntityListener(this)
    }


    companion object {
        val auxVector = Vector3()
        const val COLLISION_GROUP_PLAYER = 0x00000001
        const val COLLISION_GROUP_AI = 0x00000010
        const val COLLISION_GROUP_PLAYER_BULLET = 0x00000100
        const val COLLISION_GROUP_AI_BULLET = 0x00001000
        const val COLLISION_GROUP_GENERAL = 0x00010000
        const val COLLISION_GROUP_GROUND = 0x00100000
        const val COLLISION_GROUP_FLYING_PART = 0x01000000
    }

}
