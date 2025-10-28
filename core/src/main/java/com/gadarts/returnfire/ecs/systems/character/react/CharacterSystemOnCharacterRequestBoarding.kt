package com.gadarts.returnfire.ecs.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents

class CharacterSystemOnCharacterRequestBoarding : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val character = msg.extraInfo as Entity
        ComponentsMapper.boarding.get(character).onBoard()
        val boardingComponent =
            ComponentsMapper.boarding.get(character)
        boardingComponent.boardingAnimation?.init(gameSessionData.mapData.elevators[boardingComponent.color])
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.CHARACTER_ONBOARDING_BEGIN.ordinal,
            character
        )

    }

}
