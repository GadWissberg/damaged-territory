package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.components.TreeComponent
import com.gadarts.returnfire.components.effects.FlyingPartComponent

class MapSystemRelatedEntities(engine: PooledEngine) {
    val flyingPartEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                FlyingPartComponent::class.java,
            ).get()
        )
    }
    val treeEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                TreeComponent::class.java,
            ).get()
        )
    }

}
