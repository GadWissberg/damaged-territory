package com.gadarts.returnfire.ecs.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents.CHARACTER_REQUEST_BOARDING

class PlayerSystemOnButtonOnboardPressed :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        gamePlayManagers.dispatcher.dispatchMessage(
            CHARACTER_REQUEST_BOARDING.ordinal,
            gameSessionData.gamePlayData.player
        )
    }

}
