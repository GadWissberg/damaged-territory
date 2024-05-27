package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.systems.events.SystemEvents

abstract class GameEntitySystem : Disposable, EntitySystem(), Telegraph {
    lateinit var gameSessionData: GameSessionData
    protected lateinit var services: Services
    protected abstract val subscribedEvents: Map<SystemEvents, HandlerOnEvent>

    open fun addListener(listener: GameEntitySystem) {
        subscribedEvents.forEach { services.dispatcher.addListener(this, it.key.ordinal) }
    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        val handlerOnEvent = subscribedEvents[SystemEvents.entries[msg.message]]
        handlerOnEvent?.react(
            msg,
            gameSessionData,
            services,
        )
        return false
    }

    open fun initialize(
        gameSessionData: GameSessionData,
        services: Services
    ) {
        this.services = services
        this.gameSessionData = gameSessionData
    }

    open fun onSystemReady() {}

    abstract fun resume(delta: Long)
}
