package com.gadarts.returnfire.ecs.systems.character.react

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.character.CharacterColor
import com.gadarts.returnfire.ecs.components.pit.BaseComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.DeployableCharacters
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.shared.data.definitions.CharacterDefinition

class CharacterSystemOnMapLoaded(engine: Engine) : HandlerOnEvent {
    private val baseEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(BaseComponent::class.java).get()
    )


    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val map = gamePlayManagers.assetsManager.getAssetByDefinition(GameDebugSettings.MAP)
        baseEntities.forEach {
            val base =
                map.objects.find { placedObject ->
                    placedObject.definition.lowercase() == ComponentsMapper.amb.get(
                        it
                    ).def.name.lowercase()
                }
            val characterColor = ComponentsMapper.hangar.get(it).color
            val opponent =
                gamePlayManagers.factories.opponentCharacterFactory.create(
                    base!!,
                    getSelectedCharacter(characterColor, gameSessionData),
                    characterColor
                )
            gamePlayManagers.ecs.engine.addEntity(opponent)
            gamePlayManagers.dispatcher.dispatchMessage(
                SystemEvents.OPPONENT_CHARACTER_CREATED.ordinal,
                opponent
            )
        }

    }

    private fun getSelectedCharacter(
        characterColor: CharacterColor,
        gameSessionData: GameSessionData
    ): CharacterDefinition {
        return if (characterColor == CharacterColor.GREEN) {
            GameDebugSettings.SELECTED_VEHICLE_AI
                ?: DeployableCharacters.list[MathUtils.random(DeployableCharacters.list.size - 1)]
        } else gameSessionData.selected
    }

}
