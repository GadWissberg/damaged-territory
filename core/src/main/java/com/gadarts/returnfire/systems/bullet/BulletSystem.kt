package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData

class BulletSystem : GameEntitySystem() {
    private val bulletEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(BulletComponent::class.java).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                handleBulletCollision(PhysicsCollisionEventData.colObj0.userData as Entity)
                handleBulletCollision(PhysicsCollisionEventData.colObj1.userData as Entity)
            }
        }
    )

    private fun handleBulletCollision(entity: Entity) {
        if (ComponentsMapper.bullet.has(entity)) {
            destroyBullet(entity)
        }
    }


    override fun update(deltaTime: Float) {
        for (bullet in bulletEntities) {
            val bulletComponent = ComponentsMapper.bullet.get(bullet)
            if (bulletComponent.createdTime + 3000L > TimeUtils.millis()) {
                if (bulletComponent.behavior == BulletBehavior.CURVE) {
                    val physicsComponent = ComponentsMapper.physics.get(bullet)
                    val rotation = physicsComponent.rigidBody.worldTransform.getRotation(auxQuat)
                    if (rotation.roll > -90F) {
                        val worldTransform = physicsComponent.rigidBody.worldTransform
                        physicsComponent.rigidBody.worldTransform =
                            auxMatrix.set(worldTransform).rotate(Vector3.Z, CURVE_ROTATION_STEP)
                        val orientation = physicsComponent.rigidBody.worldTransform.getRotation(auxQuat)
                        val localZ = auxVector1.set(0F, 0F, 1F)
                        orientation.transform(localZ)
                        physicsComponent.rigidBody.linearVelocity =
                            physicsComponent.rigidBody.linearVelocity.rotate(localZ, CURVE_ROTATION_STEP)
                    }
                }
            } else {
                destroyBullet(bullet)
            }
        }
    }

    private fun destroyBullet(entity: Entity) {
        engine.removeEntity(entity)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxMatrix = Matrix4()
        private val auxVector1 = Vector3()
        private val auxQuat = Quaternion()
        private const val CURVE_ROTATION_STEP = -1.5F
    }

}
