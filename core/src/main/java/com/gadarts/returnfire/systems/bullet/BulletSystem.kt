package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData

class BulletSystem : GameEntitySystem() {
    private lateinit var bulletEntities: ImmutableArray<Entity>

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
            engine.removeEntity(entity)
        }
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        bulletEntities = engine.getEntitiesFor(Family.all(BulletComponent::class.java).get())
    }

    override fun update(deltaTime: Float) {
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }


}
