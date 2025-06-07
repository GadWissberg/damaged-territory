package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.EditorEvents

abstract class SceneHandler(private val dispatcher: MessageDispatcher) : Disposable, Telegraph {
    abstract fun update(parent: Table, deltaTime: Float)
    protected open val subscribedEvents: Map<EditorEvents, EditorOnEvent> = emptyMap()

    override fun dispose() {
    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        val handlerOnEvent = subscribedEvents[EditorEvents.entries[msg.message]]
        handlerOnEvent?.react(
            msg,
        )
        return false
    }

    open fun initialize() {
        subscribedEvents.forEach {
            dispatcher.addListener(this, it.key.ordinal)
        }
    }

}
