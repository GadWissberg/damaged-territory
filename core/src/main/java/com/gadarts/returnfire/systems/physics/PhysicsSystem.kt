package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.LimitedVelocityComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionDataMap
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_GROUND


class PhysicsSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private lateinit var ghostObject: btPairCachingGhostObject
    private lateinit var contactListener: GameContactListener

    private val limitedVelocityEntities by lazy {
        engine.getEntitiesFor(Family.all(LimitedVelocityComponent::class.java, PhysicsComponent::class.java).get())
    }

    private val bulletEngineHandler: BulletEngineHandler by lazy {
        BulletEngineHandler(
            gameSessionData,
            engine,
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                bulletEngineHandler.addBodyOfEntityToCollisionWorld(msg.extraInfo as Entity)
            }
        },
        SystemEvents.PHYSICS_COMPONENT_REMOVED_MANUALLY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val physicsComponent = msg.extraInfo as PhysicsComponent
                bulletEngineHandler.removePhysicsOfComponent(physicsComponent)
            }
        }
    )

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        bulletEngineHandler.initialize()
        contactListener = GameContactListener(gamePlayManagers.dispatcher)
    }

    override fun resume(delta: Long) {
    }

    override fun onSystemReady() {
        super.onSystemReady()
        val halfMapDepth = gameSessionData.mapData.currentMap.tilesMapping.size.toFloat() / 2F
        val halfMapWidth = gameSessionData.mapData.currentMap.tilesMapping[0].size.toFloat() / 2F
        val seaShape = btBoxShape(
            Vector3(
                halfMapWidth,
                0.02F,
                halfMapDepth,
            )
        )
        val water = gamePlayManagers.ecs.entityBuilder.begin().addGroundComponent().finishAndAddToEngine()
        ghostObject = btPairCachingGhostObject()
        ghostObject.collisionShape = seaShape
        ghostObject.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        ghostObject.worldTransform = Matrix4().translate(halfMapWidth, GameSessionDataMap.DROWNING_HEIGHT, halfMapDepth)
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
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_SYSTEM_READY.ordinal)
    }

    override fun update(deltaTime: Float) {
        bulletEngineHandler.update(deltaTime)
        val overlappingPairs = ghostObject.overlappingPairs
        val size = overlappingPairs.size()
        for (i in 4 until size) {
            gamePlayManagers.dispatcher.dispatchMessage(
                SystemEvents.PHYSICS_DROWNING.ordinal,
                overlappingPairs.atConst(i).userData
            )
        }
        for (i in 0 until limitedVelocityEntities.size()) {
            val rigidBody = ComponentsMapper.physics.get(limitedVelocityEntities[i]).rigidBody
            val limitedVelocityComponent = ComponentsMapper.limitedVelocity.get(limitedVelocityEntities[i])
            val linearVelocity = rigidBody.linearVelocity
            val maxValue = limitedVelocityComponent.maxValue
            auxVector.set(
                MathUtils.clamp(linearVelocity.x, -maxValue, maxValue),
                MathUtils.clamp(linearVelocity.y, -maxValue, maxValue),
                MathUtils.clamp(linearVelocity.z, -maxValue, maxValue),
            )
            rigidBody.linearVelocity = auxVector
        }
    }

    override fun dispose() {
        bulletEngineHandler.dispose()
        contactListener.dispose()
        ghostObject.dispose()
        engine.getEntitiesFor(Family.all(PhysicsComponent::class.java).get()).forEach {
            ComponentsMapper.physics.get(it).dispose()
        }
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
        btRigidBody.userData = gamePlayManagers.ecs.entityBuilder.begin().finishAndAddToEngine()
    }

    companion object {
        private val auxVector = Vector3()
    }
}
