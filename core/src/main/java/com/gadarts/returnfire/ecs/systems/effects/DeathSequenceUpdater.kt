package com.gadarts.returnfire.ecs.systems.effects

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.DeathSequenceComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.utils.GeneralUtils

class DeathSequenceUpdater(private val gamePlayManagers: GamePlayManagers) {
    private val deathSequenceEntities: ImmutableArray<Entity> by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(
                DeathSequenceComponent::class.java
            ).get()
        )
    }

    fun updateDeathSequences() {
        for (entity in deathSequenceEntities) {
            val deathSequenceComponent = ComponentsMapper.deathSequence.get(entity)
            if (deathSequenceComponent.deathSequenceDuration <= 0) {
                gamePlayManagers.dispatcher.dispatchMessage(
                    SystemEvents.DEATH_SEQUENCE_FINISHED.ordinal,
                    entity
                )
                entity.remove(DeathSequenceComponent::class.java)
            } else if (deathSequenceComponent.deathSequenceNextExplosion < TimeUtils.millis()) {
                deathSequenceComponent.incrementDeathSequence()
                val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
                if (ComponentsMapper.character.has(entity)) {
                    specialEffectsFactory.generateExplosionForCharacter(
                        character = entity,
                    )
                } else {
                    if (deathSequenceComponent.createExplosionsAround) {
                        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
                        val position = GeneralUtils.getRandomPositionOnBoundingBox(
                            gameModelInstance.getBoundingBox(auxBoundingBox),
                            0.5F
                        )
                        specialEffectsFactory.generateExplosion(
                            position
                        )
                    } else {
                        specialEffectsFactory.generateExplosion(
                            entity
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val auxBoundingBox = com.badlogic.gdx.math.collision.BoundingBox()
    }
}
