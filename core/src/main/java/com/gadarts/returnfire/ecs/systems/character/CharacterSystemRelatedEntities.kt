package com.gadarts.returnfire.ecs.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.ecs.components.CharacterComponent
import com.gadarts.returnfire.ecs.components.pit.HangarComponent

class CharacterSystemRelatedEntities(engine: PooledEngine) {
    val characters: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(CharacterComponent::class.java).get()
    )
    val hangars: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(HangarComponent::class.java).get()
        )
    }

}
