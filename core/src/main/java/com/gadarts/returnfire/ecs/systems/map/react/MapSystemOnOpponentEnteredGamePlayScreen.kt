package com.gadarts.returnfire.ecs.systems.map.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ElevatorComponent
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.data.OpponentEnteredGameplayScreenEventData
import com.gadarts.returnfire.ecs.systems.map.MapSystem
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.data.CharacterColor

class MapSystemOnOpponentEnteredGamePlayScreen(private val mapSystem: MapSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        if (OpponentEnteredGameplayScreenEventData.characterColor != CharacterColor.BROWN) return

        ComponentsMapper.modelInstance.get(gameSessionData.mapData.elevators[CharacterColor.BROWN]).gameModelInstance.modelInstance.transform.setTranslation(
            ModelUtils.getPositionOfModel(mapSystem.findHangar(gameSessionData.gamePlayData.player!!))
                .add(1F, ElevatorComponent.BOTTOM_EDGE_Y, 1F)
        )
    }

}
