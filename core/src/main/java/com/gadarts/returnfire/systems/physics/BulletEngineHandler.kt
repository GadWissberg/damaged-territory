package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
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
        gameSessionData.gameSessionDataPhysics.collisionWorld.debugDrawer = debugDrawer
    }


    private fun initializeBroadPhase() {
        broadPhase.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)
    }

    override fun entityAdded(entity: Entity) {
    }

    fun addBodyOfEntity(entity: Entity) {
        if (ComponentsMapper.physics.has(entity)) {
            val btRigidBody: btRigidBody = ComponentsMapper.physics.get(entity).rigidBody
            if (ComponentsMapper.bullet.has(entity)) {
                val friendly = ComponentsMapper.bullet.get(entity).friendly
                gameSessionData.gameSessionDataPhysics.collisionWorld.addRigidBody(
                    btRigidBody,
                    if (friendly) COLLISION_GROUP_PLAYER_BULLET else COLLISION_GROUP_ENEMY_BULLET,
                    0x11111111 xor (if (friendly) COLLISION_GROUP_PLAYER_BULLET else COLLISION_GROUP_ENEMY_BULLET)
                )
            } else if (ComponentsMapper.player.has(entity)) {
                gameSessionData.gameSessionDataPhysics.collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_PLAYER,
                    COLLISION_GROUP_ENEMY_BULLET or COLLISION_GROUP_ENEMY or COLLISION_GROUP_GROUND or COLLISION_GROUP_GENERAL
                )
            } else if (ComponentsMapper.enemy.has(entity)) {
                gameSessionData.gameSessionDataPhysics.collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_ENEMY,
                    COLLISION_GROUP_PLAYER_BULLET or COLLISION_GROUP_PLAYER
                )
            } else {
                gameSessionData.gameSessionDataPhysics.collisionWorld.addRigidBody(
                    btRigidBody,
                    COLLISION_GROUP_GENERAL,
                    -1
                )
            }
        }
    }

    override fun entityRemoved(entity: Entity) {
        if (ComponentsMapper.physics.has(entity)) {
            val physicsComponent = ComponentsMapper.physics[entity]
            physicsComponent.rigidBody.activationState = 0
            gameSessionData.gameSessionDataPhysics.collisionWorld.removeCollisionObject(
                physicsComponent.rigidBody
            )
            physicsComponent.dispose()
        }
    }


    private fun initializeCollisionWorld() {
        gameSessionData.gameSessionDataPhysics.collisionWorld = btDiscreteDynamicsWorld(
            dispatcher,
            broadPhase,
            solver,
            collisionConfiguration
        )
        gameSessionData.gameSessionDataPhysics.collisionWorld.gravity = Vector3(0F, -10F, 0F)
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
        gameSessionData.gameSessionDataPhysics.collisionWorld.stepSimulation(
            deltaTime,
            20,
            1f / gameSessionData.fpsTarget
        )
    }

    fun initialize() {
        Bullet.init()
        initializeBroadPhase()
        initializeCollisionWorld()
        initializeDebug()
        gameSessionData.gameSessionDataPhysics.debugDrawingMethod =
            object : CollisionShapesDebugDrawing {
                override fun drawCollisionShapes(camera: PerspectiveCamera) {
                    debugDrawer.begin(camera)
                    gameSessionData.gameSessionDataPhysics.collisionWorld.debugDrawWorld()
                    debugDrawer.end()
                }
            }
        engine.addEntityListener(this)
    }


    companion object {
        val auxVector = Vector3()
        const val COLLISION_GROUP_PLAYER = 0x00000001
        const val COLLISION_GROUP_ENEMY = 0x00000010
        const val COLLISION_GROUP_PLAYER_BULLET = 0x00000100
        const val COLLISION_GROUP_ENEMY_BULLET = 0x00001000
        const val COLLISION_GROUP_GENERAL = 0x00010000
        const val COLLISION_GROUP_GROUND = 0x00100000
    }

}
