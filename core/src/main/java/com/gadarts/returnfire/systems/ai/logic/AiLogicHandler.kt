package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.data.GameSessionData

class AiLogicHandler(
    gameSessionData: GameSessionData,
    gamePlayManagers: GamePlayManagers,
    autoAim: btPairCachingGhostObject,
    private val aiCharacterEntities: ImmutableArray<Entity>
) {
    private val aiApacheLogic by lazy {
        AiApacheLogic(
            gameSessionData,
            gamePlayManagers.dispatcher,
            gamePlayManagers.ecs.entityBuilder,
            autoAim
        )
    }
    private val aiTankLogic by lazy {
        AiTankLogic(
            gamePlayManagers.pathFinder,
            gameSessionData.mapData.mapGraph,
            gameSessionData.gamePlayData,
        )
    }

    fun update(deltaTime: Float) {
        for (character in aiCharacterEntities) {
            val boardingComponent = ComponentsMapper.boarding.get(character)
            if ((boardingComponent != null && boardingComponent.isBoarding()) || ComponentsMapper.character.get(
                    character
                ).dead
            ) continue

            if (ComponentsMapper.character.get(character).definition == SimpleCharacterDefinition.APACHE) {
                updateLogic(character, deltaTime, aiApacheLogic)
            } else {
                updateLogic(character, deltaTime, aiTankLogic)
            }
        }
    }

    private fun updateLogic(character: Entity, deltaTime: Float, logic: AiCharacterLogic) {
        logic.preUpdate(character, deltaTime)
        logic.update(character, deltaTime)
    }
}
