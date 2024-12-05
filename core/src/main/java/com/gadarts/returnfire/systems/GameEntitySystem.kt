package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

abstract class GameEntitySystem(protected val managers: Managers) : Disposable, EntitySystem(), Telegraph {
    lateinit var gameSessionData: GameSessionData
    protected open val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    open fun addListener() {
        subscribedEvents.forEach { managers.dispatcher.addListener(this, it.key.ordinal) }
    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        val handlerOnEvent = subscribedEvents[SystemEvents.entries[msg.message]]
        handlerOnEvent?.react(
            msg,
            gameSessionData,
            managers,
        )
        return false
    }

    open fun initialize(
        gameSessionData: GameSessionData,
        managers: Managers
    ) {
        this.gameSessionData = gameSessionData
    }

    open fun onSystemReady() {}

    abstract fun resume(delta: Long)
}
