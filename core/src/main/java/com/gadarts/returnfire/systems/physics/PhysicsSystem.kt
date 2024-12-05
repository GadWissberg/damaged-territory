package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_GROUND
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.auxVector


class PhysicsSystem : GameEntitySystem() {

    private lateinit var ghostObject: btPairCachingGhostObject
    private lateinit var contactListener: GameContactListener


    private val bulletEngineHandler: BulletEngineHandler by lazy {
        BulletEngineHandler(
            gameSessionData,
            engine,
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                bulletEngineHandler.addBodyOfEntityToCollisionWorld(msg.extraInfo as Entity)
            }
        },
        SystemEvents.PHYSICS_COMPONENT_REMOVED_MANUALLY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                bulletEngineHandler.removePhysicsOfComponent(msg.extraInfo as PhysicsComponent)
            }
        }
    )

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        bulletEngineHandler.initialize()
        contactListener = GameContactListener(managers.dispatcher)
    }

    override fun resume(delta: Long) {
    }

    override fun onSystemReady() {
        super.onSystemReady()
        val halfMapDepth = gameSessionData.mapData.currentMap.tilesMapping.size.toFloat() / 2F
        val halfMapWidth = gameSessionData.mapData.currentMap.tilesMapping[0].size.toFloat() / 2F
        val waterShape = btBoxShape(
            Vector3(
                halfMapWidth,
                0.02F,
                halfMapDepth,
            )
        )
        val water = EntityBuilder.begin().finishAndAddToEngine()
        ghostObject = btPairCachingGhostObject()
        ghostObject.collisionShape = waterShape
        ghostObject.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        ghostObject.worldTransform = Matrix4().translate(halfMapWidth, -1F, halfMapDepth)
        ghostObject.userData = water
        gameSessionData.physicsData.collisionWorld.addCollisionObject(
            ghostObject,
            COLLISION_GROUP_GROUND,
            -1
        )
        addBoundary(auxVector.set(1F, 0F, 0F))
        addBoundary(auxVector.set(0F, 0F, 1F))
        addBoundary(auxVector.set(-1F, 0F, 0F), -gameSessionData.mapData.currentMap.tilesMapping.size)
        addBoundary(auxVector.set(0F, 0F, -1F), -gameSessionData.mapData.currentMap.tilesMapping[0].size)
        managers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_SYSTEM_READY.ordinal)
    }

    override fun update(deltaTime: Float) {
        bulletEngineHandler.update(deltaTime)
        val overlappingPairs = ghostObject.overlappingPairs
        val size = overlappingPairs.size()
        for (i in 5 until size) {
            managers.dispatcher.dispatchMessage(
                SystemEvents.PHYSICS_DROWNING.ordinal,
                overlappingPairs.atConst(i).userData
            )
        }
    }

    override fun dispose() {
        bulletEngineHandler.dispose()
        contactListener.dispose()
        ghostObject.dispose()
    }

    private fun createBoundaryPhysicsBody(vector: Vector3, planeConstant: Int): btRigidBody {
        val ground = btStaticPlaneShape(vector, planeConstant.toFloat())
        val info = btRigidBodyConstructionInfo(
            0f,
            null,
            ground
        )
        val btRigidBody = btRigidBody(info)
        info.dispose()
        btRigidBody.collisionFlags = btCollisionObject.CollisionFlags.CF_STATIC_OBJECT
        btRigidBody.contactCallbackFlag = btBroadphaseProxy.CollisionFilterGroups.AllFilter
        return btRigidBody
    }

    private fun addBoundary(vector: Vector3, planeConstant: Int = 0) {
        val btRigidBody = createBoundaryPhysicsBody(vector, planeConstant)
        gameSessionData.physicsData.collisionWorld.addRigidBody(btRigidBody, COLLISION_GROUP_GROUND, -1)
        btRigidBody.userData = EntityBuilder.begin().addGroundComponent().finishAndAddToEngine()
    }

}
