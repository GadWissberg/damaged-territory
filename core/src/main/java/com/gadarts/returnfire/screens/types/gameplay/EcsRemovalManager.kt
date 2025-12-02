package com.gadarts.returnfire.screens.types.gameplay

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.RemoveComponentEventData

class EcsRemovalManager(private val engine: PooledEngine, dispatcher: MessageDispatcher) : Telegraph {
    private val entitiesToRemove = mutableListOf<Entity>()

    private val entitiesPendingToRemove = mutableListOf<Entity>()
    private val componentsToRemove = mutableListOf<Component>()
    private val entitiesToRemoveComponentsFrom = mutableListOf<Entity>()

    init {
        dispatcher.addListener(this, SystemEvents.REMOVE_ENTITY.ordinal)
        dispatcher.addListener(this, SystemEvents.REMOVE_COMPONENT.ordinal)
    }

    fun update() {
        if (entitiesPendingToRemove.isNotEmpty()) {
            entitiesToRemove.addAll(entitiesPendingToRemove)
            entitiesPendingToRemove.clear()
            entitiesToRemove.forEach {
                engine.removeEntity(it)
            }
            entitiesToRemove.clear()
        }
        entitiesToRemoveComponentsFrom.forEachIndexed { index, entity ->
            entity.remove(componentsToRemove[index]::class.java)
        }
        entitiesToRemoveComponentsFrom.clear()
        componentsToRemove.clear()
    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        val message = msg.message
        if (message == SystemEvents.REMOVE_ENTITY.ordinal) {
            val extraInfo = msg.extraInfo
            if (extraInfo != null) {
                val entity = extraInfo as Entity
                entitiesPendingToRemove.add(entity)
            }
        } else if (message == SystemEvents.REMOVE_COMPONENT.ordinal) {
            entitiesToRemoveComponentsFrom.add(RemoveComponentEventData.entity)
            componentsToRemove.add(RemoveComponentEventData.component)
        }

        return true
    }

}
