package com.gadarts.returnfire.systems.player.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData

class PlayerSystemOnCharacterDied : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        if (ComponentsMapper.player.has(msg.extraInfo as Entity)) {
            managers.screensManager.goToHangarScreen()
        }
    }

}
