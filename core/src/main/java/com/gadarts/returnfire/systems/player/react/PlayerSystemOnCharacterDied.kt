package com.gadarts.returnfire.systems.player.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData

class PlayerSystemOnCharacterDied : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        if (ComponentsMapper.player.has(msg.extraInfo as Entity)) {
            gameSessionData.gamePlayData.player = null
            gamePlayManagers.screensManager.goToHangarScreen()
        }
    }

}
