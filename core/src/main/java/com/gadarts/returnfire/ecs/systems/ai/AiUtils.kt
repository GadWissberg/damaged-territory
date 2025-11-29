package com.gadarts.returnfire.ecs.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.ai.logic.AiVehicleLogic.Companion.auxMatrix1
import com.gadarts.returnfire.ecs.systems.ai.logic.AiVehicleLogic.Companion.auxMatrix2
import com.gadarts.returnfire.ecs.systems.ai.logic.AiVehicleLogic.Companion.auxVector1
import com.gadarts.returnfire.ecs.systems.ai.logic.AiVehicleLogic.Companion.auxVector2

object AiUtils {
    fun findNearestRivalCharacter(
        character: Entity,
        rivalCharacters: ImmutableArray<Entity>,
        maxDistance: Float
    ): Entity? {
        var nearestEnemy: Entity? = null
        ComponentsMapper.physics.get(character).rigidBody.motionState.getWorldTransform(auxMatrix1)
        var lowestDistance = maxDistance
        for (rivalCharacter in rivalCharacters) {
            if (!ComponentsMapper.character.get(rivalCharacter).dead && (!ComponentsMapper.boarding.has(rivalCharacter) || !ComponentsMapper.boarding.get(
                    rivalCharacter
                )
                    .isBoarding()
                    )
            ) {
                val physicsComponent = ComponentsMapper.physics.get(rivalCharacter)
                physicsComponent.rigidBody.motionState.getWorldTransform(auxMatrix2)
                val enemyPosition = auxMatrix2.getTranslation(auxVector2)
                val characterPosition = auxMatrix1.getTranslation(auxVector1)
                val distanceToEnemy = characterPosition.dst2(enemyPosition)
                if (nearestEnemy == null || distanceToEnemy < lowestDistance) {
                    nearestEnemy = rivalCharacter
                    lowestDistance = distanceToEnemy
                }
            }
        }
        return nearestEnemy
    }

}
