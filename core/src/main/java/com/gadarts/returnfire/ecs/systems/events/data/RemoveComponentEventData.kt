package com.gadarts.returnfire.ecs.systems.events.data

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

object RemoveComponentEventData {
    lateinit var component: Component
        private set
    lateinit var entity: Entity
        private set

    fun set(entity: Entity, component: Component) {
        this.entity = entity
        this.component = component
    }


}
