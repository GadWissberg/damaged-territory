package com.gadarts.returnfire.ecs.systems.character.react

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.pit.HangarComponent
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.OpponentEnteredGameplayScreenEventData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

class CharacterSystemOnOpponentEnteredGamePlayScreen(engine: Engine) : HandlerOnEvent {
    private val elevatorEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(HangarComponent::class.java).get()
    )


    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val map = gamePlayManagers.assetsManager.getAssetByDefinition(GameDebugSettings.MAP)
        val colorInMessage = OpponentEnteredGameplayScreenEventData.characterColor
        val selectedElevator = elevatorEntities.first {
            val base =
                map.objects.find { placedObject ->
                    placedObject.definition.lowercase() == ComponentsMapper.amb.get(
                        it
                    ).def.name.lowercase()
                }
            val characterColor = ComponentsMapper.hangar.get(it).color
            characterColor == colorInMessage && base != null
        }
        ComponentsMapper.hangar.get(selectedElevator).open()
        val opponent =
            gamePlayManagers.factories.opponentCharacterFactory.create(
                ModelUtils.getPositionOfModel(
                    selectedElevator,
                    aux
                ),
                getSelectedCharacter(colorInMessage, gameSessionData),
                colorInMessage
            )
        gamePlayManagers.ecs.engine.addEntity(opponent)
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.OPPONENT_CHARACTER_CREATED.ordinal,
            opponent
        )
    }

    private fun getSelectedCharacter(
        characterColor: CharacterColor,
        gameSessionData: GameSessionData
    ): CharacterDefinition {
        return if (characterColor == CharacterColor.GREEN) {
            GameDebugSettings.SELECTED_VEHICLE_AI
                ?: OpponentEnteredGameplayScreenEventData.selectedCharacter
        } else gameSessionData.selectedCharacter
    }

    companion object {
        private val aux = com.badlogic.gdx.math.Vector3()
    }
}
