package com.gadarts.returnfire.systems.physics

import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.events.SystemEvents

class PhysicsSystem : GameEntitySystem() {
    private val contactListener: GameContactListener by lazy { GameContactListener() }
    private val bulletEngineHandler: BulletEngineHandler by lazy { BulletEngineHandler(gameSessionData, engine) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun resume(delta: Long) {
    }

    override fun update(deltaTime: Float) {
        bulletEngineHandler.update(deltaTime)
    }
    override fun dispose() {
        bulletEngineHandler.dispose()
        contactListener.dispose()
    }

}
