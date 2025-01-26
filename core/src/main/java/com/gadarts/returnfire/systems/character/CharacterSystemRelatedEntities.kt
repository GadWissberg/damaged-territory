package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.components.BaseComponent
import com.gadarts.returnfire.components.CharacterComponent
import com.gadarts.returnfire.components.TurretComponent

class CharacterSystemRelatedEntities(engine: Engine) {
    val baseEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(BaseComponent::class.java).get()
    )

    val charactersEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(CharacterComponent::class.java).get()
    )
    val turretEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(TurretComponent::class.java).get()
    )

}
