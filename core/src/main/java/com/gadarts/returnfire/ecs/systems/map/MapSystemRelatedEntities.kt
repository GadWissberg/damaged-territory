package com.gadarts.returnfire.ecs.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.ecs.components.TreeComponent
import com.gadarts.returnfire.ecs.components.effects.FlyingPartComponent
import com.gadarts.returnfire.ecs.components.model.ModelInstanceComponent
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent

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
    val drownableEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                ModelInstanceComponent::class.java, PhysicsComponent::class.java
            ).get()
        )
    }

}
