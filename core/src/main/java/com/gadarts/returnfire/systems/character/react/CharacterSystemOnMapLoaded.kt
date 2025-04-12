package com.gadarts.returnfire.systems.character.react

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.pit.BaseComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class CharacterSystemOnMapLoaded(engine: Engine) : HandlerOnEvent {
    private val baseEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(BaseComponent::class.java).get()
    )


    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val map = gamePlayManagers.assetsManager.getAssetByDefinition(GameDebugSettings.MAP)
        baseEntities.forEach {
            val base =
                map.placedElements.find { placedElement ->
                    placedElement.definition == ComponentsMapper.amb.get(
                        it
                    ).def
                }
            val characterColor = ComponentsMapper.hangar.get(it).color
            val opponent =
                gamePlayManagers.factories.opponentCharacterFactory.create(
                    base!!,
                    if (characterColor == CharacterColor.GREEN) GameDebugSettings.SELECTED_VEHICLE_AI else gameSessionData.selected,
                    characterColor
                )
            gamePlayManagers.ecs.engine.addEntity(opponent)
            gamePlayManagers.dispatcher.dispatchMessage(
                SystemEvents.OPPONENT_CHARACTER_CREATED.ordinal,
                opponent
            )
        }

    }

}
