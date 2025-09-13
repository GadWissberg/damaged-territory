package com.gadarts.returnfire.ecs.systems.ai.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.ai.AiSystem
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor

class AiSystemOnOpponentCharacterCreated(
    private val aiSystem: AiSystem
) :
    HandlerOnEvent {
    @Suppress("KotlinConstantConditions")
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val entity = msg.extraInfo as Entity
        val characterComponent = ComponentsMapper.character.get(entity)
        if (characterComponent.color == CharacterColor.GREEN) {
            val definition = characterComponent.definition
            gamePlayManagers.ecs.entityBuilder.addBaseAiComponentToEntity(
                entity,
                definition.getHP()
            )
            aiSystem.invokeAiComponentInitializer(definition, entity)
            if (GameDebugSettings.FORCE_ENEMY_HP >= 0) {
                ComponentsMapper.character.get(entity).hp = GameDebugSettings.FORCE_ENEMY_HP
            }
            aiSystem.getAiLogicHandler().onCharacterCreated(entity)
        }
    }

}
