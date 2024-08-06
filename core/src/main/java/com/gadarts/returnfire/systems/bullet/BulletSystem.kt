package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class BulletSystem : GameEntitySystem() {
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    private lateinit var bulletEntities: ImmutableArray<Entity>

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
