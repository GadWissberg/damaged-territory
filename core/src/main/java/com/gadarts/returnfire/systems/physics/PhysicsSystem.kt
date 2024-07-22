package com.gadarts.returnfire.systems.physics

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.auxVector

class PhysicsSystem : GameEntitySystem() {
    private val contactListener: GameContactListener by lazy { GameContactListener() }
    private val bulletEngineHandler: BulletEngineHandler by lazy {
        BulletEngineHandler(
            gameSessionData,
            engine,
            managers.dispatcher
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                bulletEngineHandler.addBodyOfEntity(gameSessionData.gameSessionDataEntities.player)
            }

        }
    )

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        bulletEngineHandler.initialize()
    }

    override fun resume(delta: Long) {
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

    override fun onSystemReady() {
        super.onSystemReady()
        val btRigidBody = createGroundPhysicsBody()
        gameSessionData.gameSessionPhysicsData.collisionWorld.addRigidBody(btRigidBody)
        btRigidBody.userData = EntityBuilder.begin().addGroundComponent().finishAndAddToEngine()
        managers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_SYSTEM_READY.ordinal)
    }

    override fun update(deltaTime: Float) {
        bulletEngineHandler.update(deltaTime)
    }

    override fun dispose() {
        bulletEngineHandler.dispose()
        contactListener.dispose()
    }

}
